package br.com.educamais.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.model.Interest;
import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.ShopItem;
import br.com.educamais.domain.model.Syllabus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** RF03, RF07, RF15, RF31: modelos de apoio (interesses, aula, ementa e loja). */
class CatalogModelTest {

    @Nested
    @DisplayName("Interest")
    class InterestTest {

        @Test
        @DisplayName("prioridade minima e 1")
        void prioridadeMinima() {
            assertThat(Interest.of(InterestCode.FOOTBALL, 0).priority()).isEqualTo(1);
            assertThat(Interest.of(InterestCode.GAMES, 5).priority()).isEqualTo(5);
        }

        @Test
        @DisplayName("interesse sem codigo e invalido")
        void codigoObrigatorio() {
            assertThatThrownBy(() -> Interest.of(null, 3))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("codigo textual e tolerante a caixa e ao nome do enum")
        void fromCode() {
            assertThat(InterestCode.fromCode("futebol")).contains(InterestCode.FOOTBALL);
            assertThat(InterestCode.fromCode("FOOTBALL")).contains(InterestCode.FOOTBALL);
            assertThat(InterestCode.fromCode(" ")).isEmpty();
            assertThat(InterestCode.fromCode("xadrez")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Lesson e Syllabus")
    class LessonTest {

        @Test
        @DisplayName("aula sem ementa continua utilizavel")
        void aulaSemEmenta() {
            Lesson lesson = new Lesson(UUID.randomUUID(), UUID.randomUUID(), "Matematica", "Fracoes", null, null);
            assertThat(lesson.hasSyllabus()).isFalse();
            assertThat(lesson.lessonDate()).isEqualTo(LocalDate.now());
            assertThat(lesson.reference()).contains("Matematica - Fracoes");
        }

        @Test
        @DisplayName("ementa com objetivos alimenta o plano")
        void ementaComObjetivos() {
            Syllabus syllabus = new Syllabus(List.of("Comparar fracoes"), List.of("fracoes"), null, "BNCC EF06MA07");
            Lesson lesson = new Lesson(UUID.randomUUID(), UUID.randomUUID(), "Matematica", "Fracoes",
                    LocalDate.of(2026, 9, 15), syllabus);

            assertThat(lesson.hasSyllabus()).isTrue();
            assertThat(syllabus.objectivesAsText()).isEqualTo("Comparar fracoes");
            assertThat(syllabus.topicsAsText()).isEqualTo("fracoes");
            assertThat(syllabus.prerequisites()).isEmpty();
        }

        @Test
        @DisplayName("listas nulas da ementa viram listas vazias")
        void ementaNula() {
            Syllabus syllabus = new Syllabus(null, null, null, null);
            assertThat(syllabus.learningObjectives()).isEmpty();
            assertThat(syllabus.hasObjectives()).isFalse();
        }
    }

    @Nested
    @DisplayName("ShopItem")
    class ShopItemTest {

        @Test
        @DisplayName("item exige codigo e preco nao negativo")
        void validacoes() {
            assertThatThrownBy(() -> new ShopItem(UUID.randomUUID(), " ", "Camisa", "roupa", 10, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ShopItem(UUID.randomUUID(), "camisa", "Camisa", "roupa", -1, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("item gratuito e valido e preserva o asset")
        void itemValido() {
            ShopItem item = new ShopItem(UUID.randomUUID(), "bone", "Bone", "acessorio", 0, "/assets/bone.png");
            assertThat(item.priceCoins()).isZero();
            assertThat(item.assetUrl()).isEqualTo("/assets/bone.png");
        }
    }
}
