package radixconv;

import groovy.util.Eval;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import javafx.application.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;

public class RadixConv extends Application implements Initializable {
    public static void main(final String[] args) throws Exception {
        launch(args);
    }

    @Override
    public void start(final Stage stage) throws IOException {
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("RadixConv.fxml"))));
        stage.show();
    }

    @FXML
    public void initialize(final URL location, final ResourceBundle resources) {
        this.outputLines.setItems(outputList);

        this.inputLine.setOnKeyPressed(this::keybind);
        this.enter.setOnAction(this::enter);
        this.clear.setOnAction(evt -> inputLine.clear());

        this.inserter.lookupAll(".number").stream().forEach(b -> ((Button)b).setOnAction(evt -> appendNumber(((Button)b).getText())));
        this.inserter.lookupAll(".notNumber").stream().forEach(b -> ((Button)b).setOnAction(evt -> inputLine.appendText(((Button)b).getText())));
        this.inserter.lookupAll("#binaryOperator Button").stream().forEach(b -> ((Button)b).setOnAction(evt -> inputLine.appendText(" " + ((Button)b).getText() + " ")));
        this.inserter.lookupAll("#unaryOperator Button").stream().forEach(b -> ((Button)b).setOnAction(evt -> inputLine.appendText(((Button)b).getText())));

        this.radix.selectedToggleProperty().addListener(this::toggleRadix);
        this.radix.selectToggle(radixDefault);

        Platform.runLater(() -> inputLine.requestFocus());
    }

    public void appendNumber(final String numberText) {
        final String line = this.inputLine.getText();
        if(line == null || line.equals("") || line.endsWith(" ")) {
            this.inputLine.appendText(Radix.valueOf(((RadioButton)this.radix.getSelectedToggle()).getText()).prefix);
        }
        this.inputLine.appendText(numberText);
    }

    private void enter(final ActionEvent evtNull) {
        final String exp = this.inputLine.getText();
        try {
            final String evaled = Eval.me("import static java.lang.Math.*;" + exp).toString();
            this.outputList.add(new Label(exp + " = " + evaled));
        }catch(Exception err) {
            this.outputList.add(new Label(err.toString()));
        }
        this.outputLines.scrollTo(outputList.size());
        this.inputLine.clear();
    }

    private void keybind(final KeyEvent key) {
        switch(key.getCode()) {
        case ENTER:
            this.enter(null);
        }
    }

    private void toggleRadix(final ObservableValue<? extends Toggle> obs, final Toggle oldValue, final Toggle newValue) {
        final Radix newRadix = Radix.valueOf(((RadioButton)newValue).getText());
        this.disableButtonFromRadix(newRadix);
        this.inputLine.setText(Radix.replaceNumber(this.inputLine.getText(), newRadix));
    }

    private void disableButtonFromRadix(final Radix radix) {
        final Set<Node> buttons = this.inserter.lookupAll(".number");
        buttons.stream().forEach(b -> ((Button)b).setDisable(true));
        buttons.stream().filter(b -> Integer.parseInt(((Button)b).getText(), 16) < radix.radix).
            forEach(b -> ((Button)b).setDisable(false));
    }

    @FXML
    private VBox document;
    @FXML
    private ListView<Label> outputLines;
    private ObservableList<Label> outputList = FXCollections.observableArrayList();
    @FXML
    private TextField inputLine;
    @FXML
    private HBox inserter;
    @FXML
    private Button enter;
    @FXML
    private Button clear;
    @FXML
    private ToggleGroup radix;
    @FXML
    private RadioButton radixDefault;
}

enum Radix {
    Bin(0b10, "0b", "((?<=(^| ))0b[0-1]+)"),
    Oct(010,  "0",  "((?<=(^| ))0[0-7]+)"),
    Dec(10,   "",   "((?<=(^| ))[1-9][0-9]+)"),
    Hex(0x10, "0x", "((?<=(^| ))0x[0-9a-f]+)");

    Radix(final Integer radix, final String prefix, final String pattern) {
        this.radix = radix;
        this.prefix = prefix;
        this.pattern = Pattern.compile(pattern);
    }

    public static String fmapWithRadix(final Radix fromR, final Radix toR, final String view) {
        return Integer.toString(Integer.parseInt(removePrefix(view), fromR.radix), toR.radix);
    }

    public static String removePrefix(final String withPrefix) {
        final Pattern p = Pattern.compile("^0[bx]?");
        return p.matcher(withPrefix).replaceAll("");
    }

    public static String replaceNumber(String line, final Radix toR) {
        for(Radix r : Radix.values()) {
            final Matcher m = r.pattern.matcher(line);
            final StringBuffer b = new StringBuffer();
            while(m.find()) {
                m.appendReplacement(b, toR.prefix + Integer.toString(Integer.parseInt(Radix.removePrefix(m.group()), r.radix), toR.radix));
            }
            line = m.appendTail(b).toString();
        }
        return line;
    }
    public Integer radix;
    public String  prefix;
    public Pattern pattern;
}
