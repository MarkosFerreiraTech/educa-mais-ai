package br.com.educamais.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RNF24: dependencias apontam para o dominio (hexagonal / DIP). */
@AnalyzeClasses(packages = "br.com.educamais", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("br.com.educamais");

    @ArchTest
    static final ArchRule dominioNaoDependeDeInfraestrutura = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "..api..", "org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule servicosNaoDependemDeControllers = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..api.controller..");

    @ArchTest
    static final ArchRule controllersNaoAcessamRepositoriosJpa = noClasses()
            .that().resideInAPackage("..api.controller..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence.jpa..");

    @ArchTest
    static final ArchRule adaptersImplementamPorts = classes()
            .that().resideInAPackage("..infrastructure.persistence.adapter..")
            .should().dependOnClassesThat().resideInAPackage("..domain.port..");

    @Test
    @DisplayName("Importacao das classes da aplicacao funciona")
    void importacaoDeClasses() {
        assertThat(CLASSES).isNotEmpty();
    }
}
