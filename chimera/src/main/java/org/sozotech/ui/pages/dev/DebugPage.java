package org.sozotech.ui.pages.dev;

import java.util.*;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import org.sozotech.ml.core.NeuralNetwork;
import org.sozotech.utils.core.AppContext;
import org.sozotech.utils.core.OpenCVContext;
import org.sozotech.utils.page.PageComponent;

public class DebugPage extends PageComponent {

    static { OpenCVContext.load(); }

    private TextArea output;
    private TextArea suggestionArea;
    private TextField input;

    private String recent_page;

    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    private List<String> currentSuggestions = new ArrayList<>();
    private int suggestionIndex = -1;

    private CommandRegistry registry;
    private CommandParser parser;
    private IntelliSenseEngine intellisense;

    private volatile boolean estimateRunning = false;
    private Thread estimateThread  = null;

    private static class ParsedCommand {
        String root;
        String option;
        String argument;
        Set<String> flags = new HashSet<>();
    }

    public static class OptionSpec {
        public final String name;
        public final List<String> args = new ArrayList<>();

        public OptionSpec(String name) { this.name = name; }

        public OptionSpec arg(String a) {
            args.add(a);
            return this;
        }
    }

    public static class CommandSpec {
        public final String name;
        private final Map<String, OptionSpec> options = new LinkedHashMap<>();
        private final List<String> flags = new ArrayList<>();

        private CommandSpec(String name) { this.name = name; }

        public CommandSpec option(String name, Consumer<OptionSpec> configure) {
            OptionSpec opt = new OptionSpec(name);
            configure.accept(opt);
            options.put(name, opt);
            return this;
        }

        public CommandSpec option(String name) {
            options.put(name, new OptionSpec(name));
            return this;
        }

        public void flag(String flag) { flags.add(flag); }

        public List<String> getOptions() { return new ArrayList<>(options.keySet()); }

        public List<String> getFlags() { return flags; }

        public List<String> getArgsFor(String option) {
            OptionSpec opt = options.get(option);
            return opt != null ? opt.args : List.of();
        }
    }

    public static class CommandRegistry {
        private final Map<String, CommandSpec> commands = new LinkedHashMap<>();

        public CommandSpec register(String name) { return commands.computeIfAbsent(name, CommandSpec::new); }

        public CommandSpec get(String name) { return commands.get(name); }

        public Set<String> commandNames() { return commands.keySet(); }
    }

    private static class CommandParser {
        ParsedCommand parse(String input) {
            String[] parts = input.trim().split("\\s+");
            ParsedCommand cmd = new ParsedCommand();
            if (parts.length == 0 || parts[0].isEmpty()) return cmd;

            cmd.root = parts[0];

            int i = 1;
            while (i < parts.length) {
                String t = parts[i];
                if (t.startsWith("--")) cmd.flags.add(t);
                else if (cmd.option == null) cmd.option = t;
                else if (cmd.argument == null) cmd.argument = t;
                i++;
            }
            return cmd;
        }
    }

    private record IntelliSenseEngine(CommandRegistry registry) {
        List<String> suggest(String input) {
            if (input == null || input.isEmpty()) return expandAll();
            boolean trailingSpace = input.endsWith(" ");
            String[] parts = input.trim().split("\\s+");

            if (parts.length == 0) return expandAll();
            if (parts.length == 1 && !trailingSpace) {
                String partial = parts[0];
                List<String> out = new ArrayList<>();
                for (String cmdName : registry.commandNames()) {
                    if (!cmdName.startsWith(partial)) continue;
                    CommandSpec spec = registry.get(cmdName);
                    List<String> expanded = expandCommand(cmdName, spec);
                    if (expanded.isEmpty()) out.add(cmdName);
                    else out.addAll(expanded);
                }
                return out;
            }

            CommandSpec spec = registry.get(parts[0]);
            if (spec == null) return List.of();

            if (parts.length == 1) return expandCommand(parts[0], spec);

            if (parts.length == 2 && !trailingSpace) {
                String partial = parts[1];
                List<String> out = new ArrayList<>();
                for (String opt : spec.getOptions()) {
                    if (!opt.startsWith(partial)) continue;
                    List<String> args = spec.getArgsFor(opt);
                    if (args.isEmpty()) out.add(parts[0] + " " + opt);
                    else for (String arg : args) out.add(parts[0] + " " + opt + " " + arg);
                }
                for (String flag : spec.getFlags()) if (flag.startsWith(partial)) out.add(parts[0] + " " + flag);
                return out;
            }

            if (parts.length == 2) {
                String opt = parts[1];
                List<String> args = spec.getArgsFor(opt);
                List<String> out = new ArrayList<>();
                if (!args.isEmpty()) for (String arg : args) out.add(parts[0] + " " + opt + " " + arg);
                else for (String flag : spec.getFlags()) out.add(parts[0] + " " + opt + " " + flag);
                return out;
            }

            if (parts.length == 3 && !trailingSpace) {
                String opt = parts[1];
                String partial = parts[2];
                List<String> out = new ArrayList<>();
                for (String arg : spec.getArgsFor(opt)) if (arg.startsWith(partial)) out.add(parts[0] + " " + opt + " " + arg);
                for (String flag : spec.getFlags()) if (flag.startsWith(partial)) out.add(parts[0] + " " + opt + " " + flag);
                return out;
            }

            return List.of();
        }

        private List<String> expandAll() {
            List<String> out = new ArrayList<>();
            for (String cmdName : registry.commandNames()) {
                CommandSpec spec = registry.get(cmdName);
                List<String> expanded = expandCommand(cmdName, spec);
                if (expanded.isEmpty()) out.add(cmdName);
                else out.addAll(expanded);
            }
            return out;
        }

        private List<String> expandCommand(String cmdName, CommandSpec spec) {
            List<String> out = new ArrayList<>();
            for (String opt : spec.getOptions()) {
                List<String> args = spec.getArgsFor(opt);
                if (args.isEmpty()) out.add(cmdName + " " + opt);
                else for (String arg : args) out.add(cmdName + " " + opt + " " + arg);
            }
            for (String flag : spec.getFlags()) out.add(cmdName + " " + flag);
            return out;
        }
    }

    @Override
    public void parameters(Map<String, Object> args) {
        this.recent_page = (String) args.get("recent-page");
    }

    private void initSystem() {
        registry = new CommandRegistry();
        parser = new CommandParser();
        registerCommands();
        intellisense = new IntelliSenseEngine(registry);
    }

    @Override
    protected Parent createView() {

        initSystem();

        VBox root = new VBox(5);
        root.setStyle("-fx-background-color: black; -fx-padding: 20;");

        output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);
        output.setFont(Font.font("Consolas", 15));
        output.setStyle(
                "-fx-control-inner-background: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-color: black;" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(output, Priority.ALWAYS);

        suggestionArea = new TextArea();
        suggestionArea.setEditable(false);
        suggestionArea.setWrapText(false);
        suggestionArea.setFont(Font.font("Consolas", 13));
        suggestionArea.setMaxHeight(140);
        suggestionArea.setPrefHeight(140);
        suggestionArea.setStyle(
                "-fx-control-inner-background: #0a0a0a;" +
                        "-fx-text-fill: #888888;" +
                        "-fx-background-color: #0a0a0a;" +
                        "-fx-border-color: transparent;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
        suggestionArea.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                suggestionArea.lookupAll(".scroll-bar").forEach(node -> node.setVisible(false));
            }
        });
        VBox.setVgrow(suggestionArea, Priority.NEVER);

        suggestionArea.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            int caretPos = suggestionArea.getCaretPosition();
            String text = suggestionArea.getText();
            if (text.isEmpty()) return;

            int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;
            int lineEnd = text.indexOf('\n', caretPos);
            if (lineEnd == -1) lineEnd = text.length();

            String clickedLine = text.substring(lineStart, lineEnd).trim();
            if (clickedLine.startsWith("> ")) clickedLine = clickedLine.substring(2);
            if (!clickedLine.isEmpty()) {
                input.setText(clickedLine);
                input.positionCaret(clickedLine.length());
                input.requestFocus();
            }
            e.consume();
        });

        input = new TextField();
        input.setFont(Font.font("Consolas", 15));
        input.setStyle(
                "-fx-background-color: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-border-color: transparent;"
        );
        input.setPromptText("type a command...");
        VBox.setVgrow(input, Priority.NEVER);

        input.textProperty().addListener((obs, oldVal, newVal) -> {
            currentSuggestions = intellisense.suggest(newVal);
            suggestionIndex = currentSuggestions.isEmpty() ? -1 : 0;
            renderSuggestions();
        });

        input.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case TAB -> {
                    if (!currentSuggestions.isEmpty()) {
                        int idx = Math.max(suggestionIndex, 0);
                        String selected = currentSuggestions.get(idx);
                        input.setText(selected + " ");
                        input.positionCaret(selected.length() + 1);
                    }
                    e.consume();
                }
                case UP -> {
                    if (!currentSuggestions.isEmpty()) {
                        if (suggestionIndex > 0) suggestionIndex--;
                        renderSuggestions();
                    } else {
                        if (commandHistory.isEmpty()) return;
                        if (historyIndex == -1) historyIndex = commandHistory.size() - 1;
                        else if (historyIndex > 0) historyIndex--;
                        String entry = commandHistory.get(historyIndex);
                        input.setText(entry);
                        input.positionCaret(entry.length());
                    }
                    e.consume();
                }
                case DOWN -> {
                    if (!currentSuggestions.isEmpty()) {
                        if (suggestionIndex < currentSuggestions.size() - 1) suggestionIndex++;
                        renderSuggestions();
                    } else {
                        if (commandHistory.isEmpty() || historyIndex == -1) return;
                        if (historyIndex < commandHistory.size() - 1) {
                            historyIndex++;
                            String entry = commandHistory.get(historyIndex);
                            input.setText(entry);
                            input.positionCaret(entry.length());
                        } else {
                            historyIndex = -1;
                            input.clear();
                        }
                    }
                    e.consume();
                }
            }
        });

        input.setOnAction(e -> {
            String command = input.getText().trim();
            if (command.isEmpty()) return;

            commandHistory.add(command);
            historyIndex = -1;

            print("> " + command);
            handleCommand(command);
            input.clear();
        });

        root.getChildren().addAll(output, suggestionArea, input);
        root.setOnMouseClicked(e -> input.requestFocus());
        Platform.runLater(() -> input.requestFocus());

        return root;
    }

    private void renderSuggestions() {
        if (currentSuggestions.isEmpty()) {
            suggestionArea.clear();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentSuggestions.size(); i++) {
            if (i == suggestionIndex) sb.append("> ").append(currentSuggestions.get(i));
            else sb.append("  ").append(currentSuggestions.get(i));

            if (i < currentSuggestions.size() - 1) sb.append("\n");
        }

        suggestionArea.setText(sb.toString());

        Platform.runLater(() -> {
            if (suggestionIndex >= 0) {
                int lineHeight = 18;
                suggestionArea.setScrollTop(Math.max(0, (suggestionIndex - 2) * lineHeight));
            }
        });
    }

    private void registerCommands() {
        registry.register("model")
                .option("train", opt -> opt.arg("--camera"))
                .option("view", opt -> opt
                        .arg("--dataset")
                        .arg("--statistics")
                        .arg("--estimate"));          // ← add this

        registry.register("estimate")
                .option("start")
                .option("stop");

        registry.register("help");
        registry.register("clear");
        registry.register("exit");
    }

    @Override
    public void onMount() {
        AppContext.router.getRenderer().lock = true;
        printBanner();
    }

    @Override
    public void onUnmount() {
        AppContext.router.getRenderer().lock = false;
        stopEstimateLoop();  // ← add this
    }

    private void print(String text) {
        output.appendText(text + "\n");
    }

    private void printBanner() {
        print(String.format("""
                ██████╗██╗  ██╗██╗███╗   ███╗███████╗██████╗  █████╗          \s
                ██╔════╝██║  ██║██║████╗ ████║██╔════╝██╔══██╗██╔══██╗         \s
                ██║     ███████║██║██╔████╔██║█████╗  ██████╔╝███████║         \s
                ██║     ██╔══██║██║██║╚██╔╝██║██╔══╝  ██╔══██╗██╔══██║             "Java Version: %s\s
                ╚██████╗██║  ██║██║██║ ╚═╝ ██║███████╗██║  ██║██║  ██║             "JavaFX Version: %s\s
                 ╚═════╝╚═╝  ╚═╝╚═╝╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝             "OpenCV Version: %s\s
               \s
                ████████╗███████╗██████╗ ███╗   ███╗██╗███╗   ██╗ █████╗ ██╗       "Type 'help' for commands"\s
                ╚══██╔══╝██╔════╝██╔══██╗████╗ ████║██║████╗  ██║██╔══██╗██║   \s
                   ██║   █████╗  ██████╔╝██╔████╔██║██║██╔██╗ ██║███████║██║   \s
                   ██║   ██╔══╝  ██╔══██╗██║╚██╔╝██║██║██║╚██╗██║██╔══██║██║   \s
                   ██║   ███████╗██║  ██║██║ ╚═╝ ██║██║██║ ╚████║██║  ██║███████╗
                   ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚══════╝
               \s""", System.getProperty("java.version"), System.getProperty("javafx.version"), org.opencv.core.Core.VERSION));
    }

    private void handleCommand(String raw) {
        ParsedCommand cmd = parser.parse(raw);
        if (cmd.root == null) return;

        switch (cmd.root) {
            case "model" -> model_cmd(cmd);
            case "help"  -> help_cmd();
            case "clear" -> clearScreen();
            case "exit"  -> exitScreen();
            default      -> print("Unknown command: " + cmd.root);
        }
    }

    private void help_cmd() {
        print("model train --camera");
        print("model view --dataset");
        print("model view --statistics");
        print("model view --estimate start   > begin live estimate loop");
        print("model view --estimate stop    > stop live estimate loop");
        print("model view --estimate         > one-shot estimate print");
        print("help");
        print("clear");
        print("exit");
    }

    private void clearScreen() {
        output.clear();
        printBanner();
    }

    private void exitScreen() {
        print("Exiting...");
        AppContext.router.navigate("/home");
    }

    private void model_cmd(ParsedCommand cmd) {
        if ("train".equals(cmd.option)) {
            if (cmd.flags.contains("--camera")) {
                print("Launching HandTrack Camera...");
                AppContext.router.navigate("/dev/media/handtrack");
                return;
            }
            print("Missing flag: --camera");
            return;
        }

        if ("view".equals(cmd.option)) {
            if (cmd.flags.contains("--dataset")) {
                print("Opening dataset viewer...");
                return;
            }
            if (cmd.flags.contains("--statistics")) {
                print("Opening statistics viewer...");
                return;
            }

            if (cmd.flags.contains("--estimate")) {
                if ("start".equals(cmd.argument)) {
                    startEstimateLoop();
                } else if ("stop".equals(cmd.argument)) {
                    stopEstimateLoop();
                } else {
                    printEstimate();
                }
                return;
            }
            print("Missing flag: --dataset or --statistics");
            return;
        }

        print("Unknown model option. Try: model train --camera, model view --dataset");
    }

    private void printEstimate() {
        NeuralNetwork.EstimateResult est = NeuralNetwork.getInstance().estimateModel();
        Platform.runLater(() -> print(est.toString()));
    }

    private void startEstimateLoop() {
        if (estimateRunning) {
            print("[estimate] Already running. Use 'model view --estimate stop' to stop.");
            return;
        }

        estimateRunning = true;
        print("[estimate] Started. Use 'model view --estimate stop' to stop.");

        estimateThread = new Thread(() -> {
            while (estimateRunning && !Thread.currentThread().isInterrupted()) {
                NeuralNetwork.EstimateResult est = NeuralNetwork.getInstance().estimateModel();
                Platform.runLater(() -> print("[estimate] " + est));
                try {
                    Thread.sleep(1000); // print every 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "chimera-estimate-loop");

        estimateThread.setDaemon(true);
        estimateThread.start();
    }

    private void stopEstimateLoop() {
        if (!estimateRunning) {
            print("[estimate] Not running.");
            return;
        }

        estimateRunning = false;
        if (estimateThread != null) estimateThread.interrupt();
        print("[estimate] Stopped.");
    }
}