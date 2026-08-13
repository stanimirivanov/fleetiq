package io.fleetiq.proto.contract;

import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import io.fleetiq.proto.common.v1.Types;
import io.fleetiq.proto.device.v1.Registry;
import io.fleetiq.proto.maintenance.v1.Predictor;
import io.fleetiq.proto.streaming.v1.FleetStreamingOuterClass;
import io.fleetiq.proto.telemetry.v1.Ingestion;
import io.fleetiq.proto.topology.v1.Topology;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProtoCompatibilityTest {

    private static final String BASELINE = "/contract/fleetiq-api.pb";

    @Test
    void publicApiRemainsBackwardCompatible() throws IOException {
        DescriptorIndex baseline = DescriptorIndex.from(loadBaseline());
        DescriptorIndex current = DescriptorIndex.from(List.of(
            Types.getDescriptor(),
            Registry.getDescriptor(),
            Predictor.getDescriptor(),
            FleetStreamingOuterClass.getDescriptor(),
            Ingestion.getDescriptor(),
            Topology.getDescriptor()
        ));

        baseline.messages.forEach((name, oldMessage) -> compareMessage(oldMessage, current.messages.get(name), name));
        baseline.enums.forEach((name, oldEnum) -> compareEnum(oldEnum, current.enums.get(name), name));
        baseline.services.forEach((name, oldService) -> compareService(oldService, current.services.get(name), name));
    }

    private FileDescriptorSet loadBaseline() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(BASELINE)) {
            assertNotNull(input, "Missing protobuf compatibility baseline " + BASELINE);
            return FileDescriptorSet.parseFrom(input);
        }
    }

    private static void compareMessage(Descriptor oldMessage, Descriptor currentMessage, String name) {
        assertNotNull(currentMessage, "Message removed or renamed: " + name);
        for (FieldDescriptor oldField : oldMessage.getFields()) {
            FieldDescriptor currentField = currentMessage.findFieldByNumber(oldField.getNumber());
            assertNotNull(currentField, "Field removed or renumbered: " + oldField.getFullName());
            assertAll("Incompatible field change: " + oldField.getFullName(),
                () -> assertEquals(oldField.getName(), currentField.getName(), "field name"),
                () -> assertEquals(oldField.getType(), currentField.getType(), "field type"),
                () -> assertEquals(oldField.isRepeated(), currentField.isRepeated(), "field cardinality"),
                () -> assertEquals(typeName(oldField), typeName(currentField), "referenced type"),
                () -> assertEquals(oneofName(oldField), oneofName(currentField), "oneof membership")
            );
        }
    }

    private static void compareEnum(EnumDescriptor oldEnum, EnumDescriptor currentEnum, String name) {
        assertNotNull(currentEnum, "Enum removed or renamed: " + name);
        for (EnumValueDescriptor oldValue : oldEnum.getValues()) {
            EnumValueDescriptor currentValue = currentEnum.findValueByNumber(oldValue.getNumber());
            assertNotNull(currentValue, "Enum value removed or renumbered: " + oldValue.getFullName());
            assertEquals(oldValue.getName(), currentValue.getName(), "Enum value renamed: " + oldValue.getFullName());
        }
    }

    private static void compareService(ServiceDescriptor oldService, ServiceDescriptor currentService, String name) {
        assertNotNull(currentService, "Service removed or renamed: " + name);
        for (MethodDescriptor oldMethod : oldService.getMethods()) {
            MethodDescriptor currentMethod = currentService.findMethodByName(oldMethod.getName());
            assertNotNull(currentMethod, "RPC removed or renamed: " + oldMethod.getFullName());
            assertAll("Incompatible RPC change: " + oldMethod.getFullName(),
                () -> assertEquals(oldMethod.getInputType().getFullName(), currentMethod.getInputType().getFullName(), "request type"),
                () -> assertEquals(oldMethod.getOutputType().getFullName(), currentMethod.getOutputType().getFullName(), "response type"),
                () -> assertEquals(oldMethod.isClientStreaming(), currentMethod.isClientStreaming(), "client streaming"),
                () -> assertEquals(oldMethod.isServerStreaming(), currentMethod.isServerStreaming(), "server streaming")
            );
        }
    }

    private static String typeName(FieldDescriptor field) {
        return switch (field.getJavaType()) {
            case MESSAGE -> field.getMessageType().getFullName();
            case ENUM -> field.getEnumType().getFullName();
            default -> "";
        };
    }

    private static String oneofName(FieldDescriptor field) {
        return field.getContainingOneof() == null ? "" : field.getContainingOneof().getName();
    }

    private static final class DescriptorIndex {
        private final Map<String, Descriptor> messages = new HashMap<>();
        private final Map<String, EnumDescriptor> enums = new HashMap<>();
        private final Map<String, ServiceDescriptor> services = new HashMap<>();

        static DescriptorIndex from(FileDescriptorSet descriptorSet) throws IOException {
            Map<String, FileDescriptor> built = new HashMap<>();
            List<com.google.protobuf.DescriptorProtos.FileDescriptorProto> remaining =
                new java.util.ArrayList<>(descriptorSet.getFileList());
            while (!remaining.isEmpty()) {
                boolean progressed = false;
                for (var candidate : List.copyOf(remaining)) {
                    if (built.keySet().containsAll(candidate.getDependencyList())) {
                        FileDescriptor[] dependencies = candidate.getDependencyList().stream()
                            .map(built::get).toArray(FileDescriptor[]::new);
                        try {
                            built.put(candidate.getName(), FileDescriptor.buildFrom(candidate, dependencies));
                        } catch (com.google.protobuf.Descriptors.DescriptorValidationException e) {
                            throw new IOException("Invalid baseline descriptor " + candidate.getName(), e);
                        }
                        remaining.remove(candidate);
                        progressed = true;
                    }
                }
                if (!progressed) {
                    throw new IOException("Could not resolve baseline descriptor dependencies: " + remaining);
                }
            }
            return from(built.values());
        }

        static DescriptorIndex from(Iterable<FileDescriptor> files) {
            DescriptorIndex index = new DescriptorIndex();
            for (FileDescriptor file : files) {
                file.getMessageTypes().forEach(message -> indexMessage(index, message));
                file.getEnumTypes().forEach(type -> index.enums.put(type.getFullName(), type));
                file.getServices().forEach(service -> index.services.put(service.getFullName(), service));
            }
            return index;
        }

        private static void indexMessage(DescriptorIndex index, Descriptor message) {
            index.messages.put(message.getFullName(), message);
            message.getNestedTypes().forEach(nested -> indexMessage(index, nested));
            message.getEnumTypes().forEach(type -> index.enums.put(type.getFullName(), type));
        }
    }
}
