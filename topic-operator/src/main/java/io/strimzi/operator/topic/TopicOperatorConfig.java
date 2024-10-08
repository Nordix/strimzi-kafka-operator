/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.topic;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strimzi.operator.common.InvalidConfigurationException;
import io.strimzi.operator.common.ReconciliationLogger;
import io.strimzi.operator.common.config.ConfigParameter;
import io.strimzi.operator.common.config.ConfigParameterParser;
import io.strimzi.operator.common.featuregates.FeatureGates;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.operator.common.model.cruisecontrol.CruiseControlApiProperties;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Topic Operator configuration.
 *
 * @param namespace                             The namespace that the operator will watch for KafkaTopics.
 * @param labelSelector                         The label selector that KafkaTopics must match.
 * @param bootstrapServers                      The Kafka bootstrap servers.
 * @param clientId                              The client Id to use for the Admin client.
 * @param fullReconciliationIntervalMs          The periodic reconciliation interval in milliseconds.
 * @param tlsEnabled                            Whether the Admin client should be configured to use TLS.
 * @param truststoreLocation                    The location (path) of the Admin client's truststore.
 * @param truststorePassword                    The password for the truststore at {@code truststoreLocation}.
 * @param keystoreLocation                      The location (path) of the Admin client's keystore.
 * @param keystorePassword                      The password for the keystore at {@code keystoreLocation}.
 * @param sslEndpointIdentificationAlgorithm    The SSL endpoint identification algorithm.
 * @param saslEnabled                           Whether the Admin client should be configured to use SASL.
 * @param saslMechanism                         The SASL mechanism for the Admin client.
 * @param saslCustomConfigJson                  The SASL custom values for the Admin client when using alternate auth mechanisms.
 * @param saslUsername                          The SASL username for the Admin client.
 * @param saslPassword                          The SASL password for the Admin client.
 * @param securityProtocol                      The security protocol for the Admin client.
 * @param useFinalizer                          Whether to use finalizers.
 * @param maxQueueSize                          The capacity of the queue.
 * @param maxBatchSize                          The maximum size of a reconciliation batch.
 * @param maxBatchLingerMs                      The maximum time to wait for a reconciliation batch to contain {@code maxBatchSize} items.
 * @param enableAdditionalMetrics               Whether to enable additional metrics.
 * @param featureGates                          Configured feature gates.
 * @param cruiseControlEnabled                  Whether Cruise Control integration is enabled.
 * @param cruiseControlRackEnabled              Whether the target Kafka cluster has rack awareness.
 * @param cruiseControlHostname                 Cruise Control hostname.
 * @param cruiseControlPort                     Cruise Control port.
 * @param cruiseControlSslEnabled               Whether Cruise Control SSL encryption is enabled.
 * @param cruiseControlAuthEnabled              Whether Cruise Control Basic authentication is enabled.
 * @param cruiseControlCrtFilePath              Certificate chain to be trusted.
 * @param cruiseControlApiUserPath              Api admin username file path.
 * @param cruiseControlApiPassPath              Api admin password file path.
 * @param alterableTopicConfig                  Comma separated list of the alterable Kafka topic properties.
 * @param skipClusterConfigReview               For some managed Kafka services the Cluster config is not callable, so this skips those calls.
 */
public record TopicOperatorConfig(
        String namespace,
        Labels labelSelector,
        String bootstrapServers,
        String clientId,
        long fullReconciliationIntervalMs,
        boolean tlsEnabled,
        String truststoreLocation,
        String truststorePassword,
        String keystoreLocation,
        String keystorePassword,
        String sslEndpointIdentificationAlgorithm,
        boolean saslEnabled,
        String saslMechanism,
        String saslCustomConfigJson,
        String saslUsername,
        String saslPassword,
        String securityProtocol,
        boolean useFinalizer,
        int maxQueueSize,
        int maxBatchSize,
        long maxBatchLingerMs,
        boolean enableAdditionalMetrics,
        FeatureGates featureGates,
        boolean cruiseControlEnabled,
        boolean cruiseControlRackEnabled,
        String cruiseControlHostname,
        int cruiseControlPort,
        boolean cruiseControlSslEnabled,
        boolean cruiseControlAuthEnabled,
        String cruiseControlCrtFilePath,
        String cruiseControlApiUserPath,
        String cruiseControlApiPassPath,
        String alterableTopicConfig,
        boolean skipClusterConfigReview
) {
    private final static ReconciliationLogger LOGGER = ReconciliationLogger.create(TopicOperatorConfig.class);
    private static final Map<String, ConfigParameter<?>> CONFIG_VALUES = new HashMap<>();
    private static final TypeReference<HashMap<String, String>> STRING_HASH_MAP_TYPE_REFERENCE = new TypeReference<>() { };
    
    /** Namespace in which the operator will run and create resources. */
    public static final ConfigParameter<String> NAMESPACE = new ConfigParameter<>("STRIMZI_NAMESPACE", ConfigParameterParser.NON_EMPTY_STRING, CONFIG_VALUES);
    /** Labels used to filter the custom resources seen by the cluster operator. */
    public static final ConfigParameter<Labels> RESOURCE_LABELS = new ConfigParameter<>("STRIMZI_RESOURCE_LABELS", ConfigParameterParser.LABEL_PREDICATE, "", CONFIG_VALUES);
    /** Kafka bootstrap address for the target Kafka cluster used by the internal admin client. */
    public static final ConfigParameter<String> BOOTSTRAP_SERVERS = new ConfigParameter<>("STRIMZI_KAFKA_BOOTSTRAP_SERVERS", ConfigParameterParser.NON_EMPTY_STRING, CONFIG_VALUES);
    /** Kafka client ID used by the internal admin client. */
    public static final ConfigParameter<String> CLIENT_ID = new ConfigParameter<>("STRIMZI_CLIENT_ID", ConfigParameterParser.NON_EMPTY_STRING, "strimzi-topic-operator-" + UUID.randomUUID(), CONFIG_VALUES);
    /** Periodic reconciliation interval in milliseconds. */
    public static final ConfigParameter<Long> FULL_RECONCILIATION_INTERVAL_MS = new ConfigParameter<>("STRIMZI_FULL_RECONCILIATION_INTERVAL_MS", ConfigParameterParser.strictlyPositive(ConfigParameterParser.LONG), "120000", CONFIG_VALUES);
    /** TLS: whether to enable configuration. */
    public static final ConfigParameter<Boolean> TLS_ENABLED = new ConfigParameter<>("STRIMZI_TLS_ENABLED", ConfigParameterParser.BOOLEAN, "false", CONFIG_VALUES);
    /** TLS: truststore location. */
    public static final ConfigParameter<String> TRUSTSTORE_LOCATION = new ConfigParameter<>("STRIMZI_TRUSTSTORE_LOCATION", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** TLS: truststore password. */
    public static final ConfigParameter<String> TRUSTSTORE_PASSWORD = new ConfigParameter<>("STRIMZI_TRUSTSTORE_PASSWORD", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** TLS: keystore location. */
    public static final ConfigParameter<String> KEYSTORE_LOCATION = new ConfigParameter<>("STRIMZI_KEYSTORE_LOCATION", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** TLS: keystore location. */
    public static final ConfigParameter<String> KEYSTORE_PASSWORD = new ConfigParameter<>("STRIMZI_KEYSTORE_PASSWORD", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** TLS: endpoint identification algorithm. */
    public static final ConfigParameter<String> SSL_ENDPOINT_IDENTIFICATION_ALGORITHM = new ConfigParameter<>("STRIMZI_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", ConfigParameterParser.STRING, "HTTPS", CONFIG_VALUES);
    /** SASL: whether to enable configuration. */
    public static final ConfigParameter<Boolean> SASL_ENABLED = new ConfigParameter<>("STRIMZI_SASL_ENABLED", ConfigParameterParser.BOOLEAN, "false", CONFIG_VALUES);
    /** SASL: mechanism. */
    public static final ConfigParameter<String> SASL_MECHANISM = new ConfigParameter<>("STRIMZI_SASL_MECHANISM", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** SASL: custom configuration. */
    public static final ConfigParameter<String> SASL_CUSTOM_CONFIG_JSON = new ConfigParameter<>("STRIMZI_SASL_CUSTOM_CONFIG_JSON", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** SASL: username. */
    public static final ConfigParameter<String> SASL_USERNAME = new ConfigParameter<>("STRIMZI_SASL_USERNAME", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** SASL: password. */
    public static final ConfigParameter<String> SASL_PASSWORD = new ConfigParameter<>("STRIMZI_SASL_PASSWORD", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** SASL: security protocol. */
    public static final ConfigParameter<String> SECURITY_PROTOCOL = new ConfigParameter<>("STRIMZI_SECURITY_PROTOCOL", ConfigParameterParser.STRING, "", CONFIG_VALUES);
    /** Whether to use finalizers for KafkaTopic resources. */
    public static final ConfigParameter<Boolean> USE_FINALIZERS = new ConfigParameter<>("STRIMZI_USE_FINALIZERS", ConfigParameterParser.BOOLEAN, "true", CONFIG_VALUES);
    /** Max topic event queue size. */
    public static final ConfigParameter<Integer> MAX_QUEUE_SIZE = new ConfigParameter<>("STRIMZI_MAX_QUEUE_SIZE", ConfigParameterParser.strictlyPositive(ConfigParameterParser.INTEGER), "1024", CONFIG_VALUES);
    /** Max size of a topic event batch. */
    public static final ConfigParameter<Integer> MAX_BATCH_SIZE = new ConfigParameter<>("STRIMZI_MAX_BATCH_SIZE", ConfigParameterParser.strictlyPositive(ConfigParameterParser.INTEGER), "100", CONFIG_VALUES);
    /** Max linger time in milliseconds before creating a new topic event batch. */
    public static final ConfigParameter<Long> MAX_BATCH_LINGER_MS = new ConfigParameter<>("STRIMZI_MAX_BATCH_LINGER_MS", ConfigParameterParser.strictlyPositive(ConfigParameterParser.LONG), "100", CONFIG_VALUES);
    /** Whether to enable additional metrics related to requests to external services (Kafka, Kubernetes, Cruise Control). */
    public static final ConfigParameter<Boolean> ENABLE_ADDITIONAL_METRICS = new ConfigParameter<>("STRIMZI_ENABLE_ADDITIONAL_METRICS", ConfigParameterParser.BOOLEAN, "false", CONFIG_VALUES);
    /** An allow list of topic configurations that are reconciles, everything else is ignored. */
    public static final ConfigParameter<String> ALTERABLE_TOPIC_CONFIG = new ConfigParameter<>("STRIMZI_ALTERABLE_TOPIC_CONFIG", ConfigParameterParser.STRING, "ALL", CONFIG_VALUES);
    /** Skip cluster level configuration checks. */
    public static final ConfigParameter<Boolean> SKIP_CLUSTER_CONFIG_REVIEW = new ConfigParameter<>("STRIMZI_SKIP_CLUSTER_CONFIG_REVIEW", ConfigParameterParser.BOOLEAN, "false", CONFIG_VALUES);
    /** List of enabled and disabled feature gates. */
    public static final ConfigParameter<FeatureGates> FEATURE_GATES = new ConfigParameter<>("STRIMZI_FEATURE_GATES", ConfigParameterParser.parseFeatureGates(), "", CONFIG_VALUES);
    /** Cruise Control: whether to enable configuration. */
    public static final ConfigParameter<Boolean> CRUISE_CONTROL_ENABLED = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_ENABLED", ConfigParameterParser.BOOLEAN, "false", CONFIG_VALUES);
    /** Cruise Control: whether rack awareness is enabled. */
    public static final ConfigParameter<Boolean> CRUISE_CONTROL_RACK_ENABLED = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_RACK_ENABLED", ConfigParameterParser.BOOLEAN, "false", CONFIG_VALUES);
    /** Cruise Control: server hostname. */
    public static final ConfigParameter<String> CRUISE_CONTROL_HOSTNAME = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_HOSTNAME", ConfigParameterParser.STRING, "127.0.0.1", CONFIG_VALUES);
    /** Cruise Control: server port. */
    public static final ConfigParameter<Integer> CRUISE_CONTROL_PORT = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_PORT", ConfigParameterParser.strictlyPositive(ConfigParameterParser.INTEGER), "9090", CONFIG_VALUES);
    /** Cruise Control: whether rack awareness is enabled. */
    public static final ConfigParameter<Boolean> CRUISE_CONTROL_SSL_ENABLED = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_SSL_ENABLED", ConfigParameterParser.BOOLEAN, "false", CONFIG_VALUES);
    /** Cruise Control: whether authentication is enabled. */
    public static final ConfigParameter<Boolean> CRUISE_CONTROL_AUTH_ENABLED = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_AUTH_ENABLED", ConfigParameterParser.BOOLEAN, "false", CONFIG_VALUES);
    /** Cruise Control: CA certificate file location. */
    public static final ConfigParameter<String> CRUISE_CONTROL_CRT_FILE_PATH = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_CRT_FILE_PATH", ConfigParameterParser.STRING, "/etc/tls-sidecar/cluster-ca-certs/ca.crt", CONFIG_VALUES);
    /** Cruise Control: username file location. */
    public static final ConfigParameter<String> CRUISE_CONTROL_API_USER_PATH = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_API_USER_PATH", ConfigParameterParser.STRING, "/etc/eto-cc-api/" + CruiseControlApiProperties.TOPIC_OPERATOR_USERNAME_KEY, CONFIG_VALUES);
    /** Cruise Control: password file location. */
    public static final ConfigParameter<String> CRUISE_CONTROL_API_PASS_PATH = new ConfigParameter<>("STRIMZI_CRUISE_CONTROL_API_PASS_PATH", ConfigParameterParser.STRING, "/etc/eto-cc-api/" + CruiseControlApiProperties.TOPIC_OPERATOR_PASSWORD_KEY, CONFIG_VALUES);

    @SuppressWarnings("unchecked")
    private static <T> T get(Map<String, Object> map, ConfigParameter<T> value) {
        return (T) map.get(value.key());
    }

    static Set<String> keyNames() {
        return Collections.unmodifiableSet(CONFIG_VALUES.keySet());
    }

    /**
     * Creates the TopicOperator configuration from a map.
     * 
     * @param map Configuration map.
     * @return TopicOperator config.
     */
    public static TopicOperatorConfig buildFromMap(Map<String, String> map) {
        Map<String, String> envMap = new HashMap<>(map);
        envMap.keySet().retainAll(TopicOperatorConfig.keyNames());

        Map<String, Object> generatedMap = ConfigParameter.define(envMap, CONFIG_VALUES);

        TopicOperatorConfig topicOperatorConfig = new TopicOperatorConfig(generatedMap);
        LOGGER.infoOp("TopicOperator configuration is {}", topicOperatorConfig);
        return topicOperatorConfig;
    }

    /**
     * Creates the TopicOperator configuration.
     * 
     * @param map Configuration map.
     */
    public TopicOperatorConfig(Map<String, Object> map) {
        this(
                get(map, NAMESPACE),
                get(map, RESOURCE_LABELS),
                get(map, BOOTSTRAP_SERVERS),
                get(map, CLIENT_ID),
                get(map, FULL_RECONCILIATION_INTERVAL_MS),
                get(map, TLS_ENABLED),
                get(map, TRUSTSTORE_LOCATION),
                get(map, TRUSTSTORE_PASSWORD),
                get(map, KEYSTORE_LOCATION),
                get(map, KEYSTORE_PASSWORD),
                get(map, SSL_ENDPOINT_IDENTIFICATION_ALGORITHM),
                get(map, SASL_ENABLED),
                get(map, SASL_MECHANISM),
                get(map, SASL_CUSTOM_CONFIG_JSON),
                get(map, SASL_USERNAME),
                get(map, SASL_PASSWORD),
                get(map, SECURITY_PROTOCOL),
                get(map, USE_FINALIZERS),
                get(map, MAX_QUEUE_SIZE),
                get(map, MAX_BATCH_SIZE),
                get(map, MAX_BATCH_LINGER_MS),
                get(map, ENABLE_ADDITIONAL_METRICS),
                get(map, FEATURE_GATES),
                get(map, CRUISE_CONTROL_ENABLED),
                get(map, CRUISE_CONTROL_RACK_ENABLED),
                get(map, CRUISE_CONTROL_HOSTNAME),
                get(map, CRUISE_CONTROL_PORT),
                get(map, CRUISE_CONTROL_SSL_ENABLED),
                get(map, CRUISE_CONTROL_AUTH_ENABLED),
                get(map, CRUISE_CONTROL_CRT_FILE_PATH),
                get(map, CRUISE_CONTROL_API_USER_PATH),
                get(map, CRUISE_CONTROL_API_PASS_PATH),
                get(map, ALTERABLE_TOPIC_CONFIG),
                get(map, SKIP_CLUSTER_CONFIG_REVIEW)
        );
    }

    Map<String, Object> adminClientConfig() {
        var kafkaClientProps = new HashMap<String, Object>();
        kafkaClientProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers());
        kafkaClientProps.put(AdminClientConfig.CLIENT_ID_CONFIG, this.clientId());

        if (this.tlsEnabled() && !this.securityProtocol().isEmpty()) {
            if (!this.securityProtocol().equals("SSL") && !this.securityProtocol().equals("SASL_SSL")) {
                throw new InvalidConfigurationException("TLS is enabled but the security protocol does not match SSL or SASL_SSL");
            }
        }

        if (!this.securityProtocol().isEmpty()) {
            kafkaClientProps.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, this.securityProtocol());
        } else if (this.tlsEnabled()) {
            kafkaClientProps.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SSL");
        } else {
            kafkaClientProps.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
        }

        if (this.securityProtocol().equals("SASL_SSL") || this.securityProtocol().equals("SSL") || this.tlsEnabled()) {
            kafkaClientProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, this.sslEndpointIdentificationAlgorithm());

            if (!this.truststoreLocation().isEmpty()) {
                kafkaClientProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, this.truststoreLocation());
            }

            if (!this.truststorePassword().isEmpty()) {
                if (this.truststoreLocation().isEmpty()) {
                    throw new InvalidConfigurationException("TLS_TRUSTSTORE_PASSWORD was supplied but TLS_TRUSTSTORE_LOCATION was not supplied");
                }
                kafkaClientProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, this.truststorePassword());
            }

            if (!this.keystoreLocation().isEmpty() && !this.keystorePassword().isEmpty()) {
                kafkaClientProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, this.keystoreLocation());
                kafkaClientProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, this.keystorePassword());
            }
        }

        if (this.saslEnabled()) {
            putSaslConfigs(kafkaClientProps);
        }

        return kafkaClientProps;
    }

    private void putSaslConfigs(Map<String, Object> kafkaClientProps) {
        TopicOperatorConfig config = this;
        String customSaslConfigJson = config.saslCustomConfigJson();

        if (customSaslConfigJson.isBlank()) {
            setStandardSaslConfigs(kafkaClientProps);
        } else {
            setCustomSaslConfigs(kafkaClientProps);
        }
    }

    private void setCustomSaslConfigs(Map<String, Object> kafkaClientProps) {
        TopicOperatorConfig config = this;
        String customPropsString = config.saslCustomConfigJson();

        if (customPropsString.isEmpty()) {
            throw new InvalidConfigurationException("Custom SASL config properties are not set");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        try {
            Map<String, String> customProperties = objectMapper.readValue(customPropsString, STRING_HASH_MAP_TYPE_REFERENCE);

            if (customProperties.isEmpty()) {
                throw new InvalidConfigurationException("SASL custom config properties empty");
            }

            for (var entry : customProperties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key == null || key.isBlank() || !key.startsWith("sasl.")) {
                    throw new InvalidConfigurationException("SASL custom config properties not SASL properties. customProperty: '" + key + "' = '" + value + "'");
                }

                kafkaClientProps.put(key, value);
            }
        } catch (JsonProcessingException e) {
            throw new InvalidConfigurationException("SASL custom config properties deserialize failed. customProperties: '" + customPropsString + "'");
        }
    }

    private void setStandardSaslConfigs(Map<String, Object> kafkaClientProps) {
        TopicOperatorConfig config = this;
        String saslMechanism;
        String jaasConfig;
        String username = config.saslUsername();
        String password = config.saslPassword();
        String configSaslMechanism = config.saslMechanism();

        if (username.isEmpty() || password.isEmpty()) {
            throw new InvalidConfigurationException("SASL credentials are not set");
        }

        if ("plain".equals(configSaslMechanism)) {
            saslMechanism = "PLAIN";
            jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + username + "\" password=\"" + password + "\";";
        } else if ("scram-sha-256".equals(configSaslMechanism) || "scram-sha-512".equals(configSaslMechanism)) {
            jaasConfig = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + username + "\" password=\"" + password + "\";";

            if ("scram-sha-256".equals(configSaslMechanism)) {
                saslMechanism = "SCRAM-SHA-256";
            } else {
                saslMechanism = "SCRAM-SHA-512";
            }
        } else {
            throw new IllegalArgumentException("Invalid SASL_MECHANISM type: " + configSaslMechanism);
        }

        kafkaClientProps.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        kafkaClientProps.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
    }

    @Override
    public String toString() {
        String mask = "********";
        return "TopicOperatorConfig{" +
                "\n\tnamespace='" + namespace + '\'' +
                "\n\tlabelSelector=" + labelSelector +
                "\n\tbootstrapServers='" + bootstrapServers + '\'' +
                "\n\tclientId='" + clientId + '\'' +
                "\n\tfullReconciliationIntervalMs=" + fullReconciliationIntervalMs +
                "\n\ttlsEnabled=" + tlsEnabled +
                "\n\ttruststoreLocation='" + truststoreLocation + '\'' +
                "\n\ttruststorePassword='" + mask + '\'' +
                "\n\tkeystoreLocation='" + keystoreLocation + '\'' +
                "\n\tkeystorePassword='" + mask + '\'' +
                "\n\tsslEndpointIdentificationAlgorithm='" + sslEndpointIdentificationAlgorithm + '\'' +
                "\n\tsaslEnabled=" + saslEnabled +
                "\n\tsaslMechanism='" + saslMechanism + '\'' +
                "\n\tsaslCustomConfigJson='" + (saslCustomConfigJson == null ? null : mask) + '\'' +
                "\n\talterableTopicConfig='" + alterableTopicConfig + '\'' +
                "\n\tskipClusterConfigReview='" + skipClusterConfigReview + '\'' +
                "\n\tsaslUsername='" + saslUsername + '\'' +
                "\n\tsaslPassword='" + mask + '\'' +
                "\n\tsecurityProtocol='" + securityProtocol + '\'' +
                "\n\tuseFinalizer=" + useFinalizer +
                "\n\tmaxQueueSize=" + maxQueueSize +
                "\n\tmaxBatchSize=" + maxBatchSize +
                "\n\tmaxBatchLingerMs=" + maxBatchLingerMs +
                "\n\tenableAdditionalMetrics=" + enableAdditionalMetrics +
                "\n\tfeatureGates='" + featureGates + "'" +
                "\n\tcruiseControlEnabled=" + cruiseControlEnabled +
                "\n\tcruiseControlRackEnabled=" + cruiseControlRackEnabled +
                "\n\tcruiseControlHostname=" + cruiseControlHostname +
                "\n\tcruiseControlPort=" + cruiseControlPort +
                "\n\tcruiseControlSslEnabled=" + cruiseControlSslEnabled +
                "\n\tcruiseControlAuthEnabled=" + cruiseControlAuthEnabled +
                "\n\tcruiseControlCrtFilePath=" + cruiseControlCrtFilePath +
                "\n\tcruiseControlApiUserPath=" + cruiseControlApiUserPath +
                "\n\tcruiseControlApiPassPath=" + cruiseControlApiPassPath +
                '}';
    }
}
