package com.cobblemonlegacy.v3;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public final class LauncherApp extends JFrame {
    private static final String CURRENT_VERSION = "3.4.15";
    private static final Color INK = new Color(27, 40, 61);
    private static final Color MUTED = new Color(82, 103, 116);
    private static final Color GREEN = new Color(34, 166, 109);
    private static final Color CYAN = new Color(171, 230, 241);
    private static final Color YELLOW = new Color(250, 198, 55);
    private static final Color RED = new Color(201, 49, 36);
    private static final Color RED_BRIGHT = new Color(255, 91, 77);
    private static final Color CREAM = new Color(255, 248, 226);
    private static final Color SKY = new Color(82, 190, 221);
    private static final Font DISPLAY_SEMIBOLD = loadFont("/ui/ChakraPetch-SemiBold.ttf", Font.BOLD);
    private static final Font DISPLAY_BOLD = loadFont("/ui/ChakraPetch-Bold.ttf", Font.BOLD);
    private static final Font BODY_FONT = loadFont("/ui/Inter.ttf", Font.PLAIN);
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private final JTextField username = new JTextField();
    private final ModeButton offlineMode = new ModeButton("PERFIL LOCAL");
    private final ModeButton microsoftMode = new ModeButton("CONTA MICROSOFT");
    private final JLabel accountState = label("Nenhuma conta Microsoft conectada.", MUTED, 10, Font.PLAIN);
    private final ActionButton playButton = new ActionButton("INICIAR AVENTURA", false);
    private final ActionButton uninstallButton = new ActionButton("DESINSTALAR", true);
    private final JCheckBox rememberMe = new JCheckBox("Lembrar de mim?");
    private final JCheckBox weakPcMode = new JCheckBox("PC Fraco");
    private final LocalNicknameStore localNickname = new LocalNicknameStore(Path.of(
            System.getProperty("user.home"), ".cobblemon_legacy_launcher", "local-nickname.txt"));
    private final StatusCard statusCard = new StatusCard();
    private final MicrosoftAuthService microsoft = new MicrosoftAuthService();
    private final BackendRuntime backendRuntime = new BackendRuntime();
    private final UpdateService updates = new UpdateService();
    private volatile AuthMode authMode = AuthMode.OFFLINE;
    private volatile MicrosoftAuthService.Account microsoftAccount;
    private volatile SwingWorker<MicrosoftAuthService.Account, Void> microsoftLoginWorker;
    private volatile JDialog microsoftLoginDialog;
    private volatile Process backendProcess;
    private volatile boolean busy;

    private enum AuthMode { OFFLINE, MICROSOFT }

    public static void main(String[] args) {
        if (args.length == 1 && "--backend-probe".equals(args[0])) {
            try {
                BackendRuntime.Prepared prepared = new BackendRuntime().prepare(message -> {});
                Process probe = new ProcessBuilder(prepared.node().toString(), "-e",
                        "const e=require('eml-lib');const n=require('prismarine-nbt');" +
                                "const p=require('./performance-profile');" +
                                "const d=require('./server-error-diagnostics');" +
                                "if(!e.Launcher||!n||!p.detectPerformanceProfile||!d.analyzeConnectionFailure)process.exit(2)")
                        .directory(prepared.backend().getParent().toFile()).inheritIO().start();
                if (probe.waitFor() != 0) throw new IllegalStateException("Dependências internas indisponíveis.");
                System.out.println("BACKEND-PROBE OK: núcleo incorporado e Node disponíveis.");
                return;
            } catch (Exception error) {
                error.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length == 1 && "--auth-probe".equals(args[0])) {
            try {
                if (!new MicrosoftAuthService().probeDeviceCode()) throw new IllegalStateException("Código Microsoft incompleto.");
                System.out.println("AUTH-PROBE OK: fluxo Microsoft/Xbox disponível.");
                return;
            } catch (Exception error) {
                error.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length == 1 && "--self-test".equals(args[0])) {
            try {
                Path temporary = Files.createTempDirectory("cobblemon-launcher-v3-test-");
                Path options = temporary.resolve("options.txt");
                Files.writeString(options, "lang:en_us\nkey_key.forward:key.keyboard.w\n", StandardCharsets.UTF_8);
                GamePreferences.forceBrazilianPortuguese(temporary);
                String updated = Files.readString(options, StandardCharsets.UTF_8);
                String normalized = updated.replace("\r\n", "\n");
                if (!normalized.startsWith("version:3955\n") || !normalized.contains("lang:pt_br")
                        || !normalized.contains("key_key.forward:key.keyboard.w")) {
                    throw new IllegalStateException("Falha ao preparar versão, idioma e atalhos nas opções.");
                }
                Object json = MiniJson.parse("{\"ok\":true,\"items\":[1,\"pt_br\"]}");
                if (!(json instanceof java.util.Map<?, ?>)) throw new IllegalStateException("Falha no leitor JSON.");
                if (!UpdateService.isNewer("3.4.16", "3.4.15")
                        || UpdateService.isNewer("3.4.15", "3.4.15")
                        || UpdateService.isNewer("3.4.14", "3.4.15")) {
                    throw new IllegalStateException("Falha na comparação de versões do atualizador.");
                }
                LocalNicknameStore nicknameStore = new LocalNicknameStore(temporary.resolve("nickname.txt"));
                nicknameStore.save("Treinador_42");
                if (!"Treinador_42".equals(nicknameStore.load())) {
                    throw new IllegalStateException("Falha ao lembrar o nickname local.");
                }
                nicknameStore.save("NovoNome");
                if (!"NovoNome".equals(nicknameStore.load())) {
                    throw new IllegalStateException("Falha ao atualizar o nickname local.");
                }
                nicknameStore.forget();
                if (nicknameStore.load() != null) {
                    throw new IllegalStateException("Falha ao esquecer o nickname local.");
                }
                try {
                    nicknameStore.save("nome inválido");
                    throw new IllegalStateException("Nickname inválido foi salvo.");
                } catch (IllegalArgumentException expected) {
                    // Somente nicknames válidos podem ser gravados.
                }
                try {
                    new MicrosoftAuthService().signIn((verificationUri, userCode) -> {}, () -> true);
                    throw new IllegalStateException("O login Microsoft ignorou o cancelamento.");
                } catch (CancellationException expected) {
                    // O cancelamento deve terminar antes de qualquer requisição de rede.
                }
                Files.delete(options);
                Files.delete(temporary);
                System.out.println("SELF-TEST OK: Java 17+, JSON, updater, options 3955, pt_br, nickname local e cancelamento Microsoft.");
                return;
            } catch (Exception error) {
                error.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length == 0 && UpdateService.detachCurrent()) return;

        Path previousJar = null;
        for (int i = 0; i + 1 < args.length; i++) {
            if ("--updated-from".equals(args[i])) {
                try {
                    previousJar = Path.of(args[++i]).toAbsolutePath().normalize();
                } catch (RuntimeException ignored) {
                    // Um argumento inválido nunca deve impedir o launcher de abrir.
                }
            }
        }
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Path cleanupTarget = previousJar;
        SwingUtilities.invokeLater(() -> {
            new LauncherApp().setVisible(true);
            if (cleanupTarget != null) UpdateService.deletePreviousWhenPossible(cleanupTarget);
        });
    }

    private LauncherApp() {
        super("Cobblemon Legacy Launcher");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1080, 780));
        setLocationRelativeTo(null);
        setIconImage(createIcon());

        BackgroundPanel shell = new BackgroundPanel();
        shell.setLayout(new BorderLayout());
        JPanel chrome = transparentPanel(new BorderLayout());
        chrome.add(new TopBar(), BorderLayout.CENTER);
        chrome.add(new TapeStrip(), BorderLayout.SOUTH);
        shell.add(chrome, BorderLayout.NORTH);

        JPanel stage = transparentPanel(new GridBagLayout());
        stage.setBorder(new EmptyBorder(22, 34, 28, 34));
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 1;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, 24);
        stage.add(new HeroPanel(), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0;
        right.weighty = 1;
        right.anchor = GridBagConstraints.CENTER;
        right.fill = GridBagConstraints.VERTICAL;
        stage.add(createLauncherPanel(), right);
        shell.add(stage, BorderLayout.CENTER);
        setContentPane(shell);

        microsoftAccount = microsoft.cachedAccount();
        updateAuthUi();
        checkForUpdates();
    }

    private JPanel createLauncherPanel() {
        AdventureCard panel = new AdventureCard();
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(430, 650));
        panel.setMinimumSize(new Dimension(410, 610));

        JPanel content = transparentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(21, 27, 18, 27));
        content.setMaximumSize(new Dimension(430, 650));

        JLabel ribbon = label("  LAUNCHER OFICIAL  ", CREAM, 10, Font.BOLD);
        ribbon.setOpaque(true);
        ribbon.setBackground(RED);
        ribbon.setBorder(new EmptyBorder(7, 11, 7, 11));
        ribbon.setMaximumSize(ribbon.getPreferredSize());
        content.add(ribbon);
        content.add(Box.createVerticalStrut(11));

        JPanel heading = transparentPanel(new BorderLayout(18, 0));
        JPanel title = transparentPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(label("SUA JORNADA COMEÇA AQUI", GREEN, 10, Font.BOLD));
        title.add(Box.createVerticalStrut(3));
        title.add(label("Entre. Sincronize. Jogue!", INK, 21, Font.BOLD));
        heading.add(title, BorderLayout.CENTER);
        JPanel pillHolder = transparentPanel(new GridBagLayout());
        pillHolder.add(new OnlinePill());
        heading.add(pillHolder, BorderLayout.EAST);
        content.add(heading);
        content.add(Box.createVerticalStrut(15));

        content.add(label("COMO DESEJA ENTRAR?", INK, 11, Font.BOLD));
        content.add(Box.createVerticalStrut(9));
        JPanel modes = transparentPanel(new GridLayout(1, 2, 8, 0));
        modes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        offlineMode.addActionListener(event -> { authMode = AuthMode.OFFLINE; updateAuthUi(); });
        microsoftMode.addActionListener(event -> {
            authMode = AuthMode.MICROSOFT;
            updateAuthUi();
            if (microsoftAccount == null) connectMicrosoft();
        });
        modes.add(offlineMode);
        modes.add(microsoftMode);
        content.add(modes);
        content.add(Box.createVerticalStrut(8));
        accountState.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountState.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        accountState.setToolTipText("Clique para entrar ou desconectar a conta Microsoft");
        accountState.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (busy || authMode != AuthMode.MICROSOFT) return;
                if (microsoftAccount == null) connectMicrosoft(); else disconnectMicrosoft();
            }
        });
        content.add(accountState);
        content.add(Box.createVerticalStrut(12));

        content.add(label("NOME DO TREINADOR", INK, 11, Font.BOLD));
        content.add(Box.createVerticalStrut(10));
        username.setFont(font(15, Font.BOLD));
        username.setForeground(INK);
        username.setCaretColor(GREEN);
        username.setOpaque(false);
        username.setBorder(new EmptyBorder(0, 46, 0, 58));
        try {
            String savedNickname = localNickname.load();
            if (savedNickname != null) {
                username.setText(savedNickname);
                rememberMe.setSelected(true);
            }
        } catch (IOException error) {
            statusCard.update("error", "Não foi possível ler o nickname salvo: " + error.getMessage(), 0);
        }
        username.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { persistLocalNickname(); }
            @Override public void removeUpdate(DocumentEvent event) { persistLocalNickname(); }
            @Override public void changedUpdate(DocumentEvent event) { persistLocalNickname(); }
        });
        FieldPanel fieldPanel = new FieldPanel(username);
        fieldPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(fieldPanel);
        content.add(Box.createVerticalStrut(8));
        content.add(label("Apenas letras, números e underscore.", MUTED, 11, Font.PLAIN));
        content.add(Box.createVerticalStrut(2));
        JPanel profileLine = transparentPanel(new BorderLayout());
        rememberMe.setOpaque(false);
        rememberMe.setContentAreaFilled(false);
        rememberMe.setFocusPainted(false);
        rememberMe.setForeground(MUTED);
        rememberMe.setFont(font(10, Font.BOLD));
        rememberMe.setToolTipText("Salvar apenas o nickname deste perfil local neste computador");
        rememberMe.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rememberMe.addActionListener(event -> persistLocalNickname());
        profileLine.add(rememberMe, BorderLayout.WEST);
        weakPcMode.setOpaque(false);
        weakPcMode.setContentAreaFilled(false);
        weakPcMode.setFocusPainted(false);
        weakPcMode.setForeground(MUTED);
        weakPcMode.setFont(font(10, Font.BOLD));
        weakPcMode.setToolTipText("Usar os gráficos mínimos recomendados e memória ajustada ao seu PC");
        weakPcMode.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileLine.add(weakPcMode, BorderLayout.EAST);
        content.add(profileLine);
        content.add(Box.createVerticalStrut(8));

        playButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        playButton.addActionListener(event -> startGame());
        content.add(playButton);
        content.add(Box.createVerticalStrut(10));
        uninstallButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        uninstallButton.addActionListener(event -> uninstall());
        content.add(uninstallButton);
        content.add(Box.createVerticalStrut(13));

        statusCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statusCard);
        content.add(Box.createVerticalStrut(11));

        JPanel footer = transparentPanel(new BorderLayout());
        footer.add(label("AUTO-SYNC · PT-BR · DESEMPENHO AUTOMÁTICO", MUTED, 9, Font.BOLD), BorderLayout.WEST);
        footer.add(label("VERSÃO 3.4.15", MUTED, 9, Font.BOLD), BorderLayout.EAST);
        content.add(footer);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(content, constraints);
        return panel;
    }

    private void checkForUpdates() {
        statusCard.update("working", "Verificando se existe uma nova versão...", 3);
        new SwingWorker<UpdateService.Release, Void>() {
            @Override protected UpdateService.Release doInBackground() throws Exception {
                return updates.latest();
            }

            @Override protected void done() {
                try {
                    UpdateService.Release release = get();
                    if (release != null && UpdateService.isNewer(release.version(), CURRENT_VERSION)) {
                        offerUpdate(release);
                    } else if (!busy) {
                        statusCard.update("status", "Launcher atualizado. Tudo pronto para jogar.", 0);
                    }
                } catch (Exception ignored) {
                    if (!busy) statusCard.update("status",
                            "Não foi possível verificar atualizações. Você ainda pode jogar.", 0);
                }
            }
        }.execute();
    }

    private void offerUpdate(UpdateService.Release release) {
        JPanel message = new JPanel();
        message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
        message.setBorder(new EmptyBorder(8, 10, 8, 10));
        message.add(new JLabel("Uma nova versão do Cobblemon Legacy Launcher está disponível."));
        message.add(Box.createVerticalStrut(10));
        message.add(new JLabel("Instalada: " + CURRENT_VERSION + "     Nova: " + release.version()));
        message.add(Box.createVerticalStrut(7));
        message.add(new JLabel("Você pode atualizar agora ou continuar usando esta versão."));
        Object[] options = {"Atualizar agora", "Continuar nesta versão"};
        int answer = JOptionPane.showOptionDialog(this, message, "Atualização disponível",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[1]);
        if (answer != 0) {
            statusCard.update("status", "Atualização adiada. Inicie normalmente quando desejar.", 0);
            return;
        }
        installUpdate(release);
    }

    private void installUpdate(UpdateService.Release release) {
        setBusy(true);
        statusCard.update("working", "Baixando a versão " + release.version() + "...", 5);
        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception {
                Path downloaded = updates.download(release, value -> SwingUtilities.invokeLater(() ->
                        statusCard.update("progress", "Baixando a versão " + release.version() + "...", value)));
                SwingUtilities.invokeLater(() -> statusCard.update(
                        "success", "Atualização concluída. Abrindo a nova versão...", 100));
                updates.launch(downloaded);
                return downloaded;
            }

            @Override protected void done() {
                try {
                    get();
                    dispose();
                    System.exit(0);
                } catch (Exception error) {
                    setBusy(false);
                    statusCard.update("error", "Falha ao atualizar: " + rootMessage(error), 0);
                }
            }
        }.execute();
    }

    private void startGame() {
        if (busy) return;
        String nickname = username.getText().trim();
        boolean weakPcSelected = weakPcMode.isSelected();
        if (authMode == AuthMode.OFFLINE && !USERNAME.matcher(nickname).matches()) {
            statusCard.update("error", "Use de 3 a 16 letras, números ou underscore.", 0);
            username.requestFocusInWindow();
            return;
        }
        if (authMode == AuthMode.OFFLINE) persistLocalNickname();

        setBusy(true);
        statusCard.update("working", "Verificando os componentes do modpack...", 0);
        AtomicBoolean receivedError = new AtomicBoolean(false);
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                MicrosoftAuthService.Account account = null;
                if (authMode == AuthMode.MICROSOFT) {
                    account = refreshOrSignIn();
                    microsoftAccount = account;
                    SwingUtilities.invokeLater(LauncherApp.this::updateAuthUi);
                }
                GamePreferences.forceBrazilianPortuguese(instancePath());
                BackendRuntime.Prepared prepared = backendRuntime.prepare(message ->
                        SwingUtilities.invokeLater(() -> statusCard.update("working", message, 5)));
                String mode = authMode == AuthMode.MICROSOFT ? "microsoft" : "offline";
                String identity = authMode == AuthMode.MICROSOFT ? microsoft.sessionFile().toString() : nickname;
                ProcessBuilder builder = new ProcessBuilder(prepared.node().toString(), prepared.backend().toString(),
                        mode, identity, weakPcSelected ? "low" : "auto");
                builder.directory(prepared.backend().getParent().toFile());
                builder.redirectErrorStream(true);
                backendProcess = builder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        backendProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("@@COBBLEMON\t")) parseBackendEvent(line, receivedError);
                    }
                }
                return backendProcess.waitFor();
            }

            @Override
            protected void done() {
                try {
                    int code = get();
                    if (code == 0 && !receivedError.get()) {
                        statusCard.update("success", "Jogo encerrado. Até a próxima aventura!", 100);
                    } else if (!receivedError.get()) {
                        statusCard.update("error", "O núcleo do launcher encerrou com o código " + code + ".", 0);
                    }
                } catch (Exception error) {
                    statusCard.update("error", friendlyError(error), 0);
                } finally {
                    backendProcess = null;
                    setBusy(false);
                }
            }
        }.execute();
    }

    private MicrosoftAuthService.Account refreshOrSignIn() throws Exception {
        if (microsoft.cachedAccount() != null) {
            try { return microsoft.refreshSaved(); }
            catch (Exception error) {
                microsoftAccount = null;
                SwingUtilities.invokeLater(this::updateAuthUi);
                throw new IOException("Sessão Microsoft expirada. Clique em conectar e tente novamente.", error);
            }
        }
        throw new IOException("Conecte sua conta Microsoft antes de iniciar o jogo.");
    }

    private void connectMicrosoft() {
        if (busy) return;
        setBusy(true);
        statusCard.update("working", "Iniciando login seguro da Microsoft...", 5);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        SwingWorker<MicrosoftAuthService.Account, Void> worker = new SwingWorker<>() {
            @Override protected MicrosoftAuthService.Account doInBackground() throws Exception {
                return microsoft.signIn((verificationUri, userCode) ->
                        showDeviceCode(verificationUri, userCode, this, cancelled), cancelled::get);
            }
            @Override protected void done() {
                if (this != microsoftLoginWorker) return;
                closeMicrosoftLoginDialog();
                try {
                    if (isCancelled() || cancelled.get()) throw new CancellationException();
                    microsoftAccount = get();
                    statusCard.update("success", "Conta " + microsoftAccount.name() + " conectada com sucesso.", 100);
                } catch (CancellationException error) {
                    finishCancelledMicrosoftLogin();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    finishCancelledMicrosoftLogin();
                } catch (ExecutionException error) {
                    if (error.getCause() instanceof CancellationException
                            || error.getCause() instanceof InterruptedException) {
                        finishCancelledMicrosoftLogin();
                    } else {
                        statusCard.update("error", rootMessage(error), 0);
                    }
                } catch (Exception error) {
                    statusCard.update("error", rootMessage(error), 0);
                } finally {
                    microsoftLoginWorker = null;
                    setBusy(false);
                    updateAuthUi();
                }
            }
        };
        microsoftLoginWorker = worker;
        worker.execute();
    }

    private void disconnectMicrosoft() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Desconectar a conta " + microsoftAccount.name() + " deste launcher?",
                "Sair da conta Microsoft", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            microsoft.logout();
            microsoftAccount = null;
            statusCard.update("success", "Conta Microsoft desconectada.", 100);
        } catch (IOException error) {
            statusCard.update("error", "Não foi possível desconectar: " + error.getMessage(), 0);
        }
        updateAuthUi();
    }

    private void showDeviceCode(String verificationUri, String userCode,
                                SwingWorker<MicrosoftAuthService.Account, Void> worker,
                                AtomicBoolean cancelled) {
        if (cancelled.get() || worker.isCancelled() || worker != microsoftLoginWorker) return;
        boolean browserOpened = openBrowser(verificationUri);
        SwingUtilities.invokeLater(() -> {
            if (cancelled.get() || worker.isCancelled() || worker != microsoftLoginWorker) return;
            JTextField code = new JTextField(userCode);
            code.setEditable(false);
            code.setHorizontalAlignment(JTextField.CENTER);
            code.setFont(font(22, Font.BOLD));
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(new JLabel(browserOpened
                    ? "O navegador foi aberto. Entre com sua conta Microsoft e use este código:"
                    : "Não foi possível abrir o navegador. Abra o endereço abaixo e use este código:"));
            panel.add(Box.createVerticalStrut(12));
            panel.add(code);
            panel.add(Box.createVerticalStrut(10));
            JTextField address = new JTextField(verificationUri);
            address.setEditable(false);
            panel.add(address);
            panel.setBorder(new EmptyBorder(18, 20, 12, 20));

            JButton openButton = new JButton("Abrir navegador");
            openButton.addActionListener(event -> {
                openButton.setEnabled(false);
                new SwingWorker<Boolean, Void>() {
                    @Override protected Boolean doInBackground() {
                        return openBrowser(verificationUri);
                    }

                    @Override protected void done() {
                        openButton.setEnabled(true);
                        try {
                            if (!get()) statusCard.update("error",
                                    "Não foi possível abrir o navegador. Copie o endereço exibido.", 0);
                        } catch (Exception error) {
                            statusCard.update("error", "Não foi possível abrir o navegador.", 0);
                        }
                    }
                }.execute();
            });
            JButton cancelButton = new JButton("Cancelar login");
            cancelButton.addActionListener(event -> cancelMicrosoftLogin(worker, cancelled));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
            actions.add(openButton);
            actions.add(cancelButton);

            JDialog dialog = new JDialog(this, "Login original Minecraft", Dialog.ModalityType.MODELESS);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    cancelMicrosoftLogin(worker, cancelled);
                }
            });
            JPanel contents = new JPanel(new BorderLayout());
            contents.add(panel, BorderLayout.CENTER);
            contents.add(actions, BorderLayout.SOUTH);
            dialog.setContentPane(contents);
            dialog.pack();
            dialog.setResizable(false);
            dialog.setLocationRelativeTo(this);
            closeMicrosoftLoginDialog();
            microsoftLoginDialog = dialog;
            dialog.setVisible(true);
        });
    }

    private void cancelMicrosoftLogin(SwingWorker<MicrosoftAuthService.Account, Void> worker,
                                      AtomicBoolean cancelled) {
        if (worker != microsoftLoginWorker) return;
        cancelled.set(true);
        microsoftLoginWorker = null;
        worker.cancel(true);
        closeMicrosoftLoginDialog();
        finishCancelledMicrosoftLogin();
        setBusy(false);
        updateAuthUi();
    }

    private void finishCancelledMicrosoftLogin() {
        authMode = AuthMode.OFFLINE;
        statusCard.update("status", "Login Microsoft cancelado. Você pode continuar com o perfil local.", 0);
    }

    private void closeMicrosoftLoginDialog() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::closeMicrosoftLoginDialog);
            return;
        }
        JDialog dialog = microsoftLoginDialog;
        microsoftLoginDialog = null;
        if (dialog != null) dialog.dispose();
    }

    private static boolean openBrowser(String location) {
        final URI uri;
        try {
            uri = URI.create(location);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
        } catch (RuntimeException error) {
            return false;
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[][] commands;
        if (os.contains("win")) {
            commands = new String[][]{{"rundll32", "url.dll,FileProtocolHandler", uri.toString()}};
        } else if (os.contains("mac")) {
            commands = new String[][]{{"open", uri.toString()}};
        } else {
            commands = new String[][]{
                    {"xdg-open", uri.toString()},
                    {"gio", "open", uri.toString()},
                    {"sensible-browser", uri.toString()}
            };
        }

        for (String[] command : commands) {
            try {
                Process process = new ProcessBuilder(command).start();
                if (!process.waitFor(1500, TimeUnit.MILLISECONDS) || process.exitValue() == 0) return true;
            } catch (Exception ignored) {
                // Tenta o próximo mecanismo disponível no sistema.
            }
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return true;
            }
        } catch (Exception ignored) {
            // O endereço continua disponível para cópia manual na janela de login.
        }
        return false;
    }

    private void updateAuthUi() {
        if (!SwingUtilities.isEventDispatchThread()) { SwingUtilities.invokeLater(this::updateAuthUi); return; }
        boolean microsoftSelected = authMode == AuthMode.MICROSOFT;
        offlineMode.setSelected(!microsoftSelected);
        microsoftMode.setSelected(microsoftSelected);
        username.setEnabled(!busy && !microsoftSelected);
        rememberMe.setEnabled(!busy && !microsoftSelected);
        accountState.setForeground(microsoftAccount == null ? MUTED : GREEN);
        accountState.setText(microsoftAccount == null
                ? (microsoftSelected ? "Clique para conectar sua conta original." : "Você também pode usar uma conta original.")
                : "✓  " + microsoftAccount.name() + (microsoftSelected ? " · clique para desconectar" : " · conta salva"));
    }

    private void parseBackendEvent(String line, AtomicBoolean receivedError) {
        String[] parts = line.split("\\t", 5);
        if (parts.length != 5) return;
        try {
            String type = parts[1];
            long current = Long.parseLong(parts[2]);
            long total = Long.parseLong(parts[3]);
            String message = new String(Base64.getDecoder().decode(parts[4]), StandardCharsets.UTF_8);
            int progress = total > 0 ? (int) Math.min(100, Math.round(current * 100.0 / total)) : -1;
            if ("running".equals(type)) progress = 100;
            if ("error".equals(type)) receivedError.set(true);
            int finalProgress = progress;
            SwingUtilities.invokeLater(() -> statusCard.update(type, message, finalProgress));
        } catch (RuntimeException ignored) {
            // Uma linha externa nunca deve interromper o launcher.
        }
    }

    private static Path instancePath() {
        return Path.of(System.getProperty("user.home"), ".cobblemon_legacy").toAbsolutePath().normalize();
    }

    private void uninstall() {
        if (busy) return;
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path instance = instancePath();
        if (!home.equals(instance.getParent()) || !".cobblemon_legacy".equals(instance.getFileName().toString())) {
            statusCard.update("error", "O diretório da instância não é seguro para exclusão.", 0);
            return;
        }

        Object[] choices = {"Cancelar", "Desinstalar"};
        int answer = JOptionPane.showOptionDialog(this,
                "Todos os mods, configurações, mundos e arquivos em\n" + instance +
                        "\nserão excluídos permanentemente.",
                "Excluir toda a instância do Minecraft?", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, choices, choices[0]);
        if (answer != 1) {
            statusCard.update("status", "Desinstalação cancelada. Nenhum arquivo foi excluído.", 0);
            return;
        }

        setBusy(true);
        statusCard.update("working", "Desinstalando a instância do Minecraft...", 25);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (!Files.exists(instance)) return false;
                Files.walkFileTree(instance, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
                        if (error != null) throw error;
                        Files.delete(directory);
                        return FileVisitResult.CONTINUE;
                    }
                });
                return true;
            }

            @Override
            protected void done() {
                try {
                    boolean removed = get();
                    statusCard.update("success", removed
                            ? "Instância do Minecraft desinstalada com sucesso."
                            : "A instância do Minecraft já estava desinstalada.", 100);
                } catch (Exception error) {
                    statusCard.update("error", "Não foi possível desinstalar: " + rootMessage(error), 0);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        busy = value;
        username.setEnabled(!value && authMode == AuthMode.OFFLINE);
        offlineMode.setEnabled(!value);
        microsoftMode.setEnabled(!value);
        rememberMe.setEnabled(!value && authMode == AuthMode.OFFLINE);
        weakPcMode.setEnabled(!value);
        playButton.setEnabled(!value);
        uninstallButton.setEnabled(!value);
        playButton.setText(value ? "AGUARDE..." : "INICIAR AVENTURA");
    }

    private void persistLocalNickname() {
        try {
            String nickname = username.getText().trim();
            if (rememberMe.isSelected() && USERNAME.matcher(nickname).matches()) {
                localNickname.save(nickname);
            } else {
                localNickname.forget();
            }
        } catch (IOException error) {
            statusCard.update("error", "Não foi possível salvar o nickname local: " + error.getMessage(), 0);
        }
    }

    private static String friendlyError(Exception error) {
        String message = rootMessage(error);
        return message.isBlank() ? "Não foi possível iniciar o launcher." : message;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    private static JPanel transparentPanel() {
        return transparentPanel(new FlowLayout());
    }

    private static JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JLabel label(String text, Color color, int size, int style) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font(size, style));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static Font font(int size, int style) {
        Font base = style == Font.PLAIN ? BODY_FONT : (style == Font.BOLD ? DISPLAY_BOLD : DISPLAY_SEMIBOLD);
        return base.deriveFont(style, (float) size);
    }

    private static Font loadFont(String resource, int fallbackStyle) {
        try (InputStream input = LauncherApp.class.getResourceAsStream(resource)) {
            if (input != null) return Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (Exception ignored) {
            // A interface continua funcional com a fonte do sistema.
        }
        return new Font("SansSerif", fallbackStyle, 12);
    }

    private static Image createIcon() {
        int size = 64;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBall(graphics, 4, 4, 56);
        graphics.dispose();
        return image;
    }

    private static void drawBall(Graphics2D graphics, int x, int y, int size) {
        Shape circle = new Ellipse2D.Float(x, y, size, size);
        graphics.setColor(new Color(241, 101, 92));
        graphics.fill(circle);
        graphics.setClip(new Rectangle(x, y + size / 2, size, size / 2));
        graphics.setColor(new Color(245, 247, 235));
        graphics.fill(circle);
        graphics.setClip(null);
        graphics.setStroke(new BasicStroke(Math.max(3, size / 16f)));
        graphics.setColor(new Color(234, 249, 235));
        graphics.draw(circle);
        graphics.setColor(new Color(23, 35, 29));
        graphics.fillRect(x + 2, y + size / 2 - 3, size - 4, 7);
        graphics.fill(new Ellipse2D.Float(x + size * .36f, y + size * .36f, size * .28f, size * .28f));
        graphics.setColor(new Color(239, 248, 236));
        graphics.fill(new Ellipse2D.Float(x + size * .44f, y + size * .44f, size * .12f, size * .12f));
    }


    private static final class HeroPanel extends JPanel {
        HeroPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(730, 650));
            setMinimumSize(new Dimension(560, 610));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(3, 3, 9, 9));
            add(new BannerPanel(), BorderLayout.CENTER);

            JPanel story = new JPanel();
            story.setOpaque(false);
            story.setLayout(new BoxLayout(story, BoxLayout.Y_AXIS));
            story.setBorder(new EmptyBorder(17, 22, 18, 22));
            JLabel badge = label("  SERVIDOR #1 DO BRASIL  ", CREAM, 10, Font.BOLD);
            badge.setOpaque(true);
            badge.setBackground(RED);
            badge.setBorder(new EmptyBorder(6, 10, 6, 10));
            badge.setMaximumSize(badge.getPreferredSize());
            story.add(badge);
            story.add(Box.createVerticalStrut(8));
            story.add(label("Capture, treine, evolua e desafie!", INK, 24, Font.BOLD));
            story.add(Box.createVerticalStrut(3));
            story.add(label("Um mundo aberto de Minecraft com Cobblemon e uma comunidade pronta para jogar.", MUTED, 11, Font.PLAIN));
            story.add(Box.createVerticalStrut(11));

            JPanel versions = transparentPanel(new GridLayout(1, 3, 9, 0));
            versions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 57));
            versions.add(new VersionChip("MINECRAFT", "1.21.1"));
            versions.add(new VersionChip("FABRIC", "0.19.5"));
            versions.add(new VersionChip("COBBLEMON", "1.7.3"));
            story.add(versions);
            add(story, BorderLayout.SOUTH);
        }

        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(27, 40, 61, 75));
            g.fill(new RoundRectangle2D.Float(7, 8, getWidth() - 8, getHeight() - 9, 28, 28));
            g.setColor(new Color(255, 255, 255, 245));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 8, getHeight() - 9, 28, 28));
            g.setStroke(new BasicStroke(3));
            g.setColor(INK);
            g.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 11, getHeight() - 12, 26, 26));
            g.dispose();
            super.paintComponent(original);
        }
    }

    private static final class BannerPanel extends JPanel {
        private final BufferedImage banner;

        BannerPanel() {
            setOpaque(false);
            setMinimumSize(new Dimension(300, 300));
            BufferedImage loaded = null;
            try { loaded = ImageIO.read(LauncherApp.class.getResource("/ui/site-banner.png")); }
            catch (Exception ignored) {}
            banner = loaded;
        }

        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            Shape clip = new RoundRectangle2D.Float(2, 2, getWidth() - 12, getHeight() + 24, 24, 24);
            g.setClip(clip);
            if (banner != null) {
                double scale = Math.max((getWidth() - 10) / (double) banner.getWidth(), getHeight() / (double) banner.getHeight());
                int width = (int) Math.ceil(banner.getWidth() * scale);
                int height = (int) Math.ceil(banner.getHeight() * scale);
                int x = (getWidth() - 10 - width) / 2 + 2;
                int y = (getHeight() - height) / 2;
                g.drawImage(banner, x, y, width, height, null);
            } else {
                g.setPaint(new GradientPaint(0, 0, SKY, getWidth(), getHeight(), YELLOW));
                g.fillRect(2, 2, getWidth() - 10, getHeight());
            }
            g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 15), 0, getHeight(), new Color(27, 40, 61, 35)));
            g.fillRect(2, 2, getWidth() - 10, getHeight());
            g.setClip(null);
            g.setColor(new Color(255, 248, 226, 225));
            g.fillRoundRect(21, 18, 176, 28, 18, 18);
            g.setColor(INK);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(21, 18, 176, 28, 18, 18);
            g.setColor(RED);
            g.fillOval(33, 28, 8, 8);
            g.setFont(font(10, Font.BOLD));
            g.setColor(INK);
            g.drawString("AVENTURA • AMIZADE", 49, 36);
            g.dispose();
            super.paintComponent(original);
        }
    }

    private static final class BackgroundPanel extends JPanel {
        BackgroundPanel() { setOpaque(false); }

        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(232, 247, 244));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setPaint(new RadialGradientPaint(new Point(getWidth() / 5, getHeight() / 4),
                    Math.max(260, getWidth() / 2f), new float[]{0f, 1f},
                    new Color[]{new Color(83, 194, 223, 95), new Color(83, 194, 223, 0)}));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setPaint(new RadialGradientPaint(new Point(getWidth() * 4 / 5, getHeight()),
                    Math.max(260, getWidth() / 2f), new float[]{0f, 1f},
                    new Color[]{new Color(47, 183, 113, 72), new Color(47, 183, 113, 0)}));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(27, 40, 61, 18));
            for (int y = 7; y < getHeight(); y += 18) {
                for (int x = 7; x < getWidth(); x += 18) g.fillOval(x, y, 2, 2);
            }
            g.dispose();
            super.paintComponent(original);
        }
    }

    private static final class TopBar extends JPanel {
        TopBar() {
            setOpaque(false);
            setLayout(new BorderLayout(16, 0));
            setBorder(new EmptyBorder(10, 30, 10, 30));
            setPreferredSize(new Dimension(100, 76));

            JPanel brand = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            brand.add(new BallLogo());
            brand.add(Box.createHorizontalStrut(12));
            JPanel words = transparentPanel();
            words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
            words.add(label("LAUNCHER OFICIAL", GREEN, 9, Font.BOLD));
            JPanel line = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            line.add(label("Cobblemon ", INK, 22, Font.BOLD));
            line.add(label("Legacy", RED, 22, Font.BOLD));
            words.add(line);
            brand.add(words);
            add(brand, BorderLayout.WEST);

            JLabel navigation = label("INÍCIO     •     18 GINÁSIOS     •     9 GERAÇÕES", MUTED, 10, Font.BOLD);
            navigation.setHorizontalAlignment(SwingConstants.CENTER);
            add(navigation, BorderLayout.CENTER);

            JLabel server = label("SERVIDOR ONLINE", new Color(18, 128, 82), 10, Font.BOLD);
            server.setBorder(new EmptyBorder(0, 12, 0, 12));
            add(server, BorderLayout.EAST);
        }

        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setColor(new Color(255, 248, 226));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(INK);
            g.fillRect(0, getHeight() - 4, getWidth(), 4);
            g.dispose();
            super.paintComponent(original);
        }
    }

    private static final class TapeStrip extends JComponent {
        TapeStrip() { setPreferredSize(new Dimension(100, 12)); }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setColor(INK);
            g.fillRect(0, 0, getWidth(), getHeight());
            int segment = 30;
            Color[] colors = {RED, CREAM, SKY, CREAM};
            for (int x = -segment; x < getWidth() + segment; x += segment) {
                int index = Math.floorMod(x / segment, colors.length);
                g.setColor(colors[index]);
                Polygon stripe = new Polygon(new int[]{x, x + segment, x + segment + 10, x + 10},
                        new int[]{2, 2, getHeight() - 2, getHeight() - 2}, 4);
                g.fill(stripe);
            }
            g.dispose();
        }
    }

    private static final class AdventureCard extends JPanel {
        AdventureCard() { setOpaque(false); }

        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(27, 40, 61, 85));
            g.fill(new RoundRectangle2D.Float(7, 8, getWidth() - 8, getHeight() - 9, 28, 28));
            g.setColor(CREAM);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 8, getHeight() - 9, 28, 28));
            g.setStroke(new BasicStroke(3));
            g.setColor(INK);
            g.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 11, getHeight() - 12, 26, 26));
            g.dispose();
            super.paintComponent(original);
        }
    }

    private static final class BallLogo extends JComponent {
        BallLogo() { setPreferredSize(new Dimension(50, 50)); }
        @Override protected void paintComponent(Graphics graphics) { drawBall((Graphics2D) graphics, 1, 1, 47); }
    }

    private static final class VersionChip extends JPanel {
        VersionChip(String name, String value) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(8, 12, 8, 12));
            add(label(name, MUTED, 8, Font.BOLD));
            add(Box.createVerticalStrut(2));
            add(label(value, INK, 12, Font.BOLD));
        }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(27, 40, 61, 65));
            g.fillRoundRect(0, 4, getWidth(), getHeight() - 4, 16, 16);
            g.setColor(CREAM);
            g.fillRoundRect(0, 0, getWidth(), getHeight() - 4, 16, 16);
            g.setStroke(new BasicStroke(2));
            g.setColor(INK);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 5, 16, 16);
            g.dispose();
            super.paintComponent(original);
        }
    }

    private static final class OnlinePill extends JComponent {
        OnlinePill() { setPreferredSize(new Dimension(78, 29)); }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(226, 247, 220));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(34, 166, 109, 130));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.setColor(GREEN);
            g.fillOval(10, 11, 7, 7);
            g.setFont(font(9, Font.BOLD));
            g.setColor(new Color(18, 119, 77));
            g.drawString("ONLINE", 23, 18);
            g.dispose();
        }
    }

    private static final class FieldPanel extends JPanel {
        private final JTextField field;
        FieldPanel(JTextField field) {
            this.field = field;
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(360, 54));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            add(field);
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent event) { repaint(); }
                @Override public void focusLost(FocusEvent event) { repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(27, 40, 61, 60));
            g.fillRoundRect(0, 4, getWidth(), getHeight() - 4, 18, 18);
            g.setColor(field.hasFocus() ? new Color(213, 245, 248) : Color.WHITE);
            g.fillRoundRect(0, 0, getWidth(), getHeight() - 4, 18, 18);
            g.setStroke(new BasicStroke(field.hasFocus() ? 2.5f : 2f));
            g.setColor(field.hasFocus() ? RED : INK);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 5, 18, 18);
            g.setColor(SKY);
            g.fillOval(16, 13, 13, 13);
            g.setColor(INK);
            g.setStroke(new BasicStroke(2));
            g.drawOval(16, 13, 13, 13);
            g.drawArc(13, 28, 20, 13, 0, 180);
            g.setFont(font(9, Font.BOLD));
            g.setColor(MUTED);
            g.drawString("3–16", getWidth() - 43, 30);
            g.dispose();
            super.paintComponent(original);
        }
    }

    private static final class ModeButton extends JToggleButton {
        ModeButton(String text) {
            super(text);
            setFont(font(10, Font.BOLD));
            setForeground(INK);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(176, 40));
        }

        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(27, 40, 61, isSelected() ? 65 : 35));
            g.fillRoundRect(0, 3, getWidth(), getHeight() - 3, 16, 16);
            g.setColor(isSelected() ? CYAN : new Color(255, 255, 255, 185));
            g.fillRoundRect(0, 0, getWidth(), getHeight() - 3, 16, 16);
            g.setStroke(new BasicStroke(2));
            g.setColor(isSelected() ? RED : INK);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 4, 16, 16);
            g.dispose();
            setForeground(isEnabled() ? INK : MUTED);
            super.paintComponent(original);
        }
    }

    private static final class ActionButton extends JButton {
        private final boolean danger;
        ActionButton(String text, boolean danger) {
            super(text);
            this.danger = danger;
            setFont(font(danger ? 10 : 13, Font.BOLD));
            setForeground(danger ? RED : Color.WHITE);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(360, danger ? 40 : 54));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, danger ? 40 : 54));
        }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (danger) {
                g.setColor(new Color(27, 40, 61, 45));
                g.fillRoundRect(0, 3, getWidth(), getHeight() - 3, 18, 18);
                g.setColor(new Color(255, 255, 255, isEnabled() ? 170 : 90));
                g.fillRoundRect(0, 0, getWidth(), getHeight() - 3, 18, 18);
                g.setStroke(new BasicStroke(1.6f));
                g.setColor(new Color(201, 49, 36, isEnabled() ? 190 : 70));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 4, 18, 18);
            } else {
                g.setColor(new Color(27, 40, 61, 140));
                g.fillRoundRect(0, 5, getWidth(), getHeight() - 5, 26, 26);
                g.setPaint(isEnabled() ? new GradientPaint(0, 0, RED_BRIGHT, getWidth(), 0, RED)
                        : new Color(156, 145, 140));
                g.fillRoundRect(0, 0, getWidth(), getHeight() - 5, 26, 26);
                g.setStroke(new BasicStroke(2));
                g.setColor(INK);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 6, 26, 26);
                g.setColor(new Color(130, 26, 25));
                g.fillOval(getWidth() / 2 - 99, 13, 24, 24);
                g.setColor(Color.WHITE);
                Polygon triangle = new Polygon(new int[]{getWidth()/2-90, getWidth()/2-90, getWidth()/2-83},
                        new int[]{18, 31, 25}, 3);
                g.fill(triangle);
            }
            g.dispose();
            setForeground(danger ? (isEnabled() ? RED : MUTED) : Color.WHITE);
            super.paintComponent(original);
        }
    }

    private static final class StatusCard extends JPanel {
        private final JLabel title = label("STATUS DO LAUNCHER", INK, 10, Font.BOLD);
        private final JLabel state = label("PRONTO", GREEN, 9, Font.BOLD);
        private final JLabel message = label("Tudo certo para começar sua jornada.", MUTED, 10, Font.PLAIN);
        private final BrandProgressBar progress = new BrandProgressBar();

        StatusCard() {
            setOpaque(false);
            setLayout(new BorderLayout(0, 8));
            setBorder(new EmptyBorder(14, 16, 14, 16));
            setPreferredSize(new Dimension(360, 102));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 102));
            JPanel top = transparentPanel(new BorderLayout());
            top.add(title, BorderLayout.WEST);
            top.add(state, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
            add(message, BorderLayout.CENTER);
            progress.setPreferredSize(new Dimension(320, 7));
            add(progress, BorderLayout.SOUTH);
        }

        void update(String type, String text, int value) {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> update(type, text, value));
                return;
            }
            message.setText(text);
            message.setToolTipText(text);
            if (value >= 0) progress.setValue(value);
            Color color = "error".equals(type) ? RED : GREEN;
            title.setForeground("error".equals(type) ? RED : INK);
            state.setForeground(color);
            state.setText(switch (type) {
                case "error" -> "ATENÇÃO";
                case "success" -> "CONCLUÍDO";
                case "running" -> "EM JOGO";
                case "progress" -> value >= 0 ? value + "%" : "BAIXANDO";
                case "working" -> "PREPARANDO";
                default -> "PRONTO";
            });
        }

        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(27, 40, 61, 45));
            g.fillRoundRect(0, 4, getWidth(), getHeight() - 4, 18, 18);
            g.setColor(new Color(218, 244, 244));
            g.fillRoundRect(0, 0, getWidth(), getHeight() - 4, 18, 18);
            g.setStroke(new BasicStroke(1.7f));
            g.setColor(INK);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 5, 18, 18);
            g.dispose();
            super.paintComponent(original);
        }
    }

    private static final class BrandProgressBar extends JProgressBar {
        BrandProgressBar() {
            super(0, 100);
            setOpaque(false);
            setBorderPainted(false);
        }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = Math.max(5, getHeight());
            g.setColor(new Color(27, 40, 61, 34));
            g.fillRoundRect(0, 0, getWidth(), h, h, h);
            int fill = (int) Math.round(getWidth() * getPercentComplete());
            if (fill > 0) {
                g.setPaint(new GradientPaint(0, 0, SKY, getWidth(), 0, GREEN));
                g.fillRoundRect(0, 0, fill, h, h, h);
            }
            g.dispose();
        }
    }
}
