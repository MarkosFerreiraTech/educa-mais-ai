package br.com.educamais.infrastructure.notification;

import br.com.educamais.domain.port.NotificationPort;

/** Mensagem proativa personalizada (RF24). */
final class WhatsAppMessageTemplate {

    private WhatsAppMessageTemplate() {
    }

    static String render(NotificationPort.DeliveryRequest request, String deepLinkBase) {
        return """
                Oi, %s! 👋 Vi que voce faltou na aula de hoje.

                Conteudo perdido: %s
                Preparei uma missao curta (uns 10 minutos) com exemplos do seu interesse.

                Bora recuperar? %s/%s
                """.formatted(request.studentName(), request.topic(), deepLinkBase, request.missionLink());
    }
}
