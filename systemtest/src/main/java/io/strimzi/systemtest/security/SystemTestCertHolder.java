/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.security;

import io.fabric8.kubernetes.api.model.Secret;
import io.strimzi.operator.cluster.model.Ca;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.systemtest.storage.TestStorage;
import io.strimzi.systemtest.utils.kubeUtils.objects.SecretUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static io.strimzi.systemtest.security.SystemTestCertManager.convertPrivateKeyToPKCS8File;
import static io.strimzi.test.k8s.KubeClusterResource.kubeClient;

/**
 * Provides representation of custom chain key-pair (i.e., {@code strimziRootCa}, {@code intermediateCa},
 * {@code systemTestClusterCa} and {@code systemTestClusterCa}. Moreover, it allow to generate a new Client or Cluster
 * key-pair used for instance in renewal process.
 */
public class SystemTestCertHolder {

    private static final Logger LOGGER = LogManager.getLogger(SystemTestCertHolder.class);

    private final String caCertSecretName;
    private final String caKeySecretName;
    private final String subjectDn;

    private final SystemTestCertAndKey strimziRootCa;
    private final SystemTestCertAndKey intermediateCa;
    private final SystemTestCertAndKey systemTestCa;
    private final CertAndKeyFiles bundle;

    public SystemTestCertHolder(final String cnName, final String caCertSecretName, final String caKeySecretName) {
        this.strimziRootCa = SystemTestCertManager.generateRootCaCertAndKey();
        this.intermediateCa = SystemTestCertManager.generateIntermediateCaCertAndKey(this.strimziRootCa);
        this.subjectDn = "C=CZ, L=Prague, O=StrimziTest, " + cnName;

        this.systemTestCa = SystemTestCertManager.generateStrimziCaCertAndKey(this.intermediateCa, this.subjectDn);

        // Create PEM bundles (strimzi root CA, intermediate CA, cluster|clients CA cert+key) for ClusterCA and ClientsCA
        this.bundle = SystemTestCertManager.exportToPemFiles(this.systemTestCa, this.intermediateCa, this.strimziRootCa);

        this.caCertSecretName = caCertSecretName;
        this.caKeySecretName = caKeySecretName;
    }

    /**
     * Prepares custom Cluster and Clients key-pair and creates related Secrets with initialization of annotation
     * {@code Ca.ANNO_STRIMZI_IO_CA_CERT_GENERATION} and {@code Ca.ANNO_STRIMZI_IO_CA_KEY_GENERATION}.
     *
     * @param namespaceName name of the Namespace, where we creates such key-pair and related Secrets
     * @param clusterName name of the Kafka cluster, which is associated with generated key-pair
     */
    public void prepareCustomSecretsFromBundles(final String namespaceName, final String clusterName) {
        final Map<String, String> additionalSecretLabels = Map.of(
            Labels.STRIMZI_CLUSTER_LABEL, clusterName,
            Labels.STRIMZI_KIND_LABEL, "Kafka");

        try {
            LOGGER.info("Deploy Certificate authority ({}-{}) for Kafka cluster {}/{} and create associated Secrets",
                this.caCertSecretName, this.caKeySecretName, namespaceName, clusterName);
            // 1. Replace the CA certificate generated by the Cluster Operator.
            //      a) Delete the existing secret.
            SecretUtils.deleteSecretWithWait(this.caCertSecretName, namespaceName);
            //      b) Create the new (custom) secret.
            SecretUtils.createCustomSecret(this.caCertSecretName, clusterName, namespaceName, this.bundle);

            // 2. Replace the private key generated by the Cluster Operator and 3. Labeling of the secrets is done in creation phase
            //      a) Delete the existing secret.
            SecretUtils.deleteSecretWithWait(this.caKeySecretName, namespaceName);
            final File strimziKeyPKCS8 = convertPrivateKeyToPKCS8File(this.systemTestCa.getPrivateKey());
            //      b) Create the new (custom) secret.

            Secret secret = SecretUtils.createSecretFromFile(strimziKeyPKCS8.getAbsolutePath(), "ca.key", this.caKeySecretName, namespaceName, additionalSecretLabels);
            kubeClient().namespace(namespaceName).createSecret(secret);
            SecretUtils.waitForSecretReady(namespaceName, this.caKeySecretName, () -> { });

            // 4. Annotate the secrets
            SecretUtils.annotateSecret(namespaceName, this.caCertSecretName, Ca.ANNO_STRIMZI_IO_CA_CERT_GENERATION, "0");
            SecretUtils.annotateSecret(namespaceName, this.caKeySecretName, Ca.ANNO_STRIMZI_IO_CA_KEY_GENERATION, "0");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String retrieveOldCertificateName(final Secret s, final String dataKey) {
        final X509Certificate caCert = SecretUtils.getCertificateFromSecret(s, dataKey);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(caCert.getNotAfter().toInstant(), ZoneId.systemDefault());
        final String localDateString = localDateTime.format(DateTimeFormatter.ISO_DATE);

        // old name
        return "ca-" + localDateTime.toString().replaceAll(":", "-") + "." + dataKey.split("\\.")[1];
    }

    public static void increaseCertGenerationCounterInSecret(final Secret secret, final TestStorage testStorage, final String annotationKey) {
        Map<String, String> clusterCaSecretAnnotations = secret.getMetadata().getAnnotations();
        if (clusterCaSecretAnnotations == null) {
            clusterCaSecretAnnotations = new HashMap<>();
        }
        if (clusterCaSecretAnnotations.containsKey(annotationKey)) {
            int generationNumber = Integer.parseInt(clusterCaSecretAnnotations.get(annotationKey));
            clusterCaSecretAnnotations.put(annotationKey, String.valueOf(++generationNumber));
        }
        kubeClient(testStorage.getNamespaceName()).patchSecret(testStorage.getNamespaceName(), secret.getMetadata().getName(), secret);
    }

    public SystemTestCertAndKey getStrimziRootCa() {
        return strimziRootCa;
    }
    public SystemTestCertAndKey getIntermediateCa() {
        return intermediateCa;
    }
    public SystemTestCertAndKey getSystemTestCa() {
        return systemTestCa;
    }
    public CertAndKeyFiles getBundle() {
        return bundle;
    }
    public String getCaCertSecretName() {
        return caCertSecretName;
    }
    public String getCaKeySecretName() {
        return caKeySecretName;
    }

    public String getSubjectDn() {
        return subjectDn;
    }
}
