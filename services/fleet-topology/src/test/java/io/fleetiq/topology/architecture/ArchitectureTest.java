package io.fleetiq.topology.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.fleetiq.topology", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_adapters =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_transport_or_persistence_frameworks =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "io.fleetiq.proto..",
                "io.grpc..",
                "io.smallrye.reactive.messaging..",
                "jakarta.persistence..",
                "org.hibernate..");

    @ArchTest
    static final ArchRule inbound_adapters_do_not_call_outbound_implementations =
        noClasses().that().resideInAPackage("..adapter.inbound..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.outbound..");

    @ArchTest
    static final ArchRule outbound_adapters_do_not_call_inbound_adapters =
        noClasses().that().resideInAPackage("..adapter.outbound..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.inbound..");
}

