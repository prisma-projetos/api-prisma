package br.com.prisma.api_prisma.dto;

/**
 * Monta URLs de midia hospedada no MinIO. Nao ha coluna de URL no banco: o
 * caminho e sempre derivado de UF + sequencial (foto) ou sigla (logo), na
 * hora de montar o DTO de resposta.
 */
final class MinioUrls {

    private static final String BASE = "https://api-minio.ajvale.com.br/2026";

    private MinioUrls() {
    }

    static String fotoCandidato(String uf, Long sequencial) {
        if (uf == null || uf.isBlank() || sequencial == null) {
            return null;
        }
        String ufUpper = uf.trim().toUpperCase();
        return BASE + "/fotosCandidatos/" + ufUpper + "/F" + ufUpper + sequencial + "_div.jpg";
    }

    static String logoPartido(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            return null;
        }
        return BASE + "/fotosPartidos/" + sigla.trim().toUpperCase() + ".svg";
    }
}
