package org.mars_sim.msp.ui.javafx.demo;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.Dimension;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;

public class JavaFXBrowserWithHistory  extends Application{
	
    //private final ProgressBar progressBar = new ProgressBar();

    public static void main(String[] args){
        launch(args);
    }
    
    public void start(Stage primaryStage){                
        BorderPane ap = new BorderPane();

        Scene scene = new Scene(ap, 900, 900);

        VBox sp = new VBox();

        Button reloadButton = new Button("R");
        reloadButton.setMinWidth(30);
        reloadButton.setTooltip(new Tooltip("Reload this page"));

        Button backButton = new Button("<");
        backButton.setMinWidth(30);
        backButton.setTooltip(new Tooltip("Go back"));

        Button forwardButton = new Button(">");
        forwardButton.setMinWidth(30);
        forwardButton.setTooltip(new Tooltip("Go forward"));

        TextField tf = new TextField();
        tf.setPromptText("URL Address");
        tf.setMinWidth(1024);
        tf.setPrefWidth(1024);
        

        ComboBox comboBox = new ComboBox();
        comboBox.setPromptText("History");
        comboBox.setMaxWidth(110);

        WebView browser = new WebView();

        WebEngine webEngine = browser.getEngine();
        webEngine.load("http://www.google.com");
        webEngine.setJavaScriptEnabled(true);

/*        
        Worker<?> worker = webEngine.getLoadWorker();
        worker.workDoneProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {
                SwingUtilities.invokeLater(()->{
                	//System.out.println("workDoneProperty()");
                        progressBar.setValue(newValue.intValue());
                });
            }
        });
*/
        
        WebHistory history = webEngine.getHistory();

        Button Google = new Button("Google");
        Google.setMaxWidth(110);
        Button Yahoo = new Button("Yahoo");
        Yahoo.setMaxWidth(110);
        Button Bing = new Button("Bing");
        Bing.setMaxWidth(110);
        Button Facebook = new Button("Facebook");
        Facebook.setMaxWidth(110);
        Button Twitter = new Button("Twitter");
        Twitter.setMaxWidth(110);
        Button YouTube = new Button("YouTube");
        YouTube.setMaxWidth(110);

        history.getEntries().addListener((Change<? extends Entry> c) -> {
            c.next();
            for (Entry e : c.getRemoved()) {
                comboBox.getItems().remove(e.getUrl());
            }
            for (Entry e : c.getAddedSubList()) {
                comboBox.getItems().add(e.getUrl());
            }
        });

        comboBox.setPrefWidth(60);
        comboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent ev) {
                int offset = comboBox.getSelectionModel().getSelectedIndex() - history.getCurrentIndex();
                history.go(offset);
            }
        });

        reloadButton.setOnAction(e -> webEngine.reload());
        backButton.setOnAction(e -> webEngine.executeScript("history.back()"));
        forwardButton.setOnAction(e -> webEngine.executeScript("history.forward()"));
        Google.setOnAction(e -> webEngine.load("http://www.google.com"));
        Yahoo.setOnAction(e -> webEngine.load("http://www.yahoo.com"));
        Bing.setOnAction(e -> webEngine.load("http://www.bing.com"));
        Facebook.setOnAction(e -> webEngine.load("http://www.facebook.com"));
        Twitter.setOnAction(e -> webEngine.load("http://www.twitter.com"));
        YouTube.setOnAction(e -> webEngine.load("http://www.youtube.com"));

        tf.setOnKeyPressed((KeyEvent ke) -> {
            KeyCode key = ke.getCode();
            if(key == KeyCode.ENTER){
            	
                webEngine.load("http://" + tf.getText());
            }
        });

/*        
        VBox statusBar = new VBox();
        
        progressBar.setPreferredSize(new Dimension(150, 18));
        progressBar.setStringPainted(true);
        statusBar.getChildren().add(progressBar);
*/       
        sp.getChildren().addAll(comboBox, Google, Yahoo, Bing, Facebook, Twitter, YouTube);

        ap.setLeft(sp);
        
        HBox top = new HBox();
        top.getChildren().addAll(backButton, forwardButton, reloadButton, tf);
        
        ap.setTop(top);
        ap.setCenter(browser);

        browser.setPrefSize(700, 700);
        primaryStage.setTitle("Browser");
        primaryStage.setScene(scene);

        //String css = JavaFXBrowserWithHistory.class.getResource("Viper.css").toExternalForm();
        //scene.getStylesheets().add(css);

        primaryStage.show();
    }
    
    private static Object executejQuery(final WebEngine engine, String minVersion, String jQueryLocation, String script) {
        return engine.executeScript(
                "(function(window, document, version, callback) { "
                + "var j, d;"
                + "var loaded = false;"
                + "if (!(j = window.jQuery) || version > j.fn.jquery || callback(j, loaded)) {"
                + " var script = document.createElement(\"script\");"
                + " script.type = \"text/javascript\";"
                + " script.src = \"" + jQueryLocation + "\";"
                + " script.onload = script.onreadystatechange = function() {"
                + " if (!loaded && (!(d = this.readyState) || d == \"loaded\" || d == \"complete\")) {"
                + " callback((j = window.jQuery).noConflict(1), loaded = true);"
                + " j(script).remove();"
                + " }"
                + " };"
                + " document.documentElement.childNodes[0].appendChild(script) "
                + "} "
                + "})(window, document, \"" + minVersion + "\", function($, jquery_loaded) {" + script + "});");
    }
 
    
}
