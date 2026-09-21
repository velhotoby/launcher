package com.cobblemonlegacy.v3;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class EventBannerService {
    private static final String RESOURCE_ROOT = "/ui/";
    private static final String REMOTE_ROOT =
            "https://raw.githubusercontent.com/velhotoby/launcher/main/java-launcher-v3/src/main/resources/ui/";
    private static final Pattern EVENT_IMAGE = Pattern.compile("events/[a-z0-9][a-z0-9_-]{0,63}\\.(?:png|jpe?g)");
    private static final Pattern HASH = Pattern.compile("[a-f0-9]{64}");
    private static final int MAX_MANIFEST_BYTES = 64 * 1024;
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

    record Banner(String title, String subtitle, BufferedImage image) {}
    record Result(List<Banner> banners, boolean online) {}
    private record Spec(String title, String subtitle, String image, String sha256) {}

    private EventBannerService() {}

    static Result load() throws IOException {
        try {
            return new Result(loadRemote(), true);
        } catch (IOException | RuntimeException remoteError) {
            return new Result(loadBundled(), false);
        }
    }

    private static List<Banner> loadRemote() throws IOException {
        byte[] manifest = fetch("events.json", MAX_MANIFEST_BYTES);
        List<Spec> specs = parseSpecs(manifest);
        List<Banner> banners = new ArrayList<>();
        for (Spec spec : specs) {
            byte[] image = fetch(spec.image(), MAX_IMAGE_BYTES);
            banners.add(new Banner(spec.title(), spec.subtitle(), checkedImage(image, spec.sha256())));
        }
        return List.copyOf(banners);
    }

    private static List<Banner> loadBundled() throws IOException {
        List<Spec> specs = parseSpecs(resource("events.json", MAX_MANIFEST_BYTES));
        List<Banner> banners = new ArrayList<>();
        for (Spec spec : specs) {
            byte[] image = resource(spec.image(), MAX_IMAGE_BYTES);
            banners.add(new Banner(spec.title(), spec.subtitle(), checkedImage(image, spec.sha256())));
        }
        return List.copyOf(banners);
    }

    private static List<Spec> parseSpecs(byte[] manifest) throws IOException {
        Object parsed;
        try {
            parsed = MiniJson.parse(new String(manifest, StandardCharsets.UTF_8));
        } catch (RuntimeException error) {
            throw new IOException("Manifesto de eventos inválido.", error);
        }
        if (!(parsed instanceof Map<?, ?> root) || !(root.get("schemaVersion") instanceof Number version)
                || version.intValue() != 1 || !(root.get("events") instanceof List<?> entries)
                || entries.size() > 8) {
            throw new IOException("Formato do manifesto de eventos não suportado.");
        }
        List<Spec> specs = new ArrayList<>();
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> item)) throw new IOException("Evento inválido no manifesto.");
            String title = checkedText(item.get("title"), 80);
            String subtitle = checkedText(item.get("subtitle"), 150);
            String image = checkedText(item.get("image"), 90);
            String sha256 = checkedText(item.get("sha256"), 64);
            if (!validImagePath(image) || !HASH.matcher(sha256).matches()) {
                throw new IOException("Imagem ou SHA-256 inválido no evento.");
            }
            specs.add(new Spec(title, subtitle, image, sha256));
        }
        return List.copyOf(specs);
    }

    private static String checkedText(Object value, int limit) throws IOException {
        if (!(value instanceof String text) || text.isBlank() || text.length() > limit) {
            throw new IOException("Texto inválido no manifesto de eventos.");
        }
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (Character.isISOControl(character) || character == '<' || character == '>') {
                throw new IOException("O texto do evento contém caracteres não permitidos.");
            }
        }
        return text.trim();
    }

    private static boolean validImagePath(String image) {
        return "site-banner.png".equals(image) || EVENT_IMAGE.matcher(image).matches();
    }

    private static byte[] fetch(String path, int limit) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(REMOTE_ROOT + path))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "Cobblemon-Legacy-Launcher/3.4.29")
                .header("Cache-Control", "no-cache")
                .GET().build();
        try {
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream input = response.body()) {
                if (response.statusCode() != 200) throw new IOException("GitHub retornou HTTP " + response.statusCode());
                return limited(input, limit);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Consulta de eventos interrompida.", error);
        }
    }

    private static byte[] resource(String path, int limit) throws IOException {
        try (InputStream input = EventBannerService.class.getResourceAsStream(RESOURCE_ROOT + path)) {
            if (input == null) throw new IOException("Banner local indisponível: " + path);
            return limited(input, limit);
        }
    }

    private static byte[] limited(InputStream input, int limit) throws IOException {
        byte[] bytes = input.readNBytes(limit + 1);
        if (bytes.length > limit) throw new IOException("Arquivo de evento excede o limite permitido.");
        return bytes;
    }

    private static BufferedImage checkedImage(byte[] bytes, String expectedHash) throws IOException {
        String actualHash;
        try {
            actualHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IOException("SHA-256 indisponível.", error);
        }
        if (!actualHash.equals(expectedHash)) throw new IOException("SHA-256 do banner não confere.");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IOException("Formato de imagem inválido.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Imagem de evento não reconhecida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 300 || height < 150 || width > 5000 || height > 3000
                        || (long) width * height > 12_000_000) {
                    throw new IOException("Dimensões do banner fora dos limites.");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    static void selfTest() throws IOException {
        List<Banner> banners = loadBundled();
        if (banners.size() != 1 || !"Evento de teste".equals(banners.get(0).title())
                || banners.get(0).image().getWidth() != 1983
                || validImagePath("../segredo.png") || !validImagePath("events/torneio.png")) {
            throw new IOException("Falha no catálogo de eventos de teste.");
        }
    }
}
