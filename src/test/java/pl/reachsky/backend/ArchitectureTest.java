package pl.reachsky.backend;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "pl.reachsky.backend")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring_or_jpa =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .orShould().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .as("Domain classes must not depend on Spring or JPA");

    @ArchTest
    static final ArchRule transactional_annotation_only_in_application_layer =
            noMethods().that().areDeclaredInClassesThat().resideOutsideOfPackage("..application..")
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .as("@Transactional is only allowed in the application layer");

    @ArchTest
    static final ArchRule spring_data_repositories_must_be_package_private =
            classes().that().haveNameMatching(".*SpringDataRepository")
                    .should().bePackagePrivate()
                    .as("Spring Data repository interfaces must be package-private to prevent direct use outside the adapter");

    @ArchTest
    static final ArchRule in_adapters_must_not_depend_on_out_adapters =
            noClasses().that().resideInAPackage("..adapter.in..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
                    .as("Inbound adapters must not depend on outbound adapters");
}
