package com.josejo911;

import javax.print.PrintException;
import javax.servlet.annotation.WebServlet;

import com.josejo911.antlr.CustomErrorListener;
import com.josejo911.semanticControl.SemanticListener;
import com.josejo911.antlr.decafLexer;
import com.josejo911.antlr.decafParser;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.*;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Window;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * IDE UI for the Decaf Compiler
 * @author Javier Jo
 * Universidad del Valle de Guatemala, Construccion de Compiladores
 */
@Theme("mytheme")
public class MyUI extends UI {
    private String editorInput; // actual input to compile
    private ParseTree grammarParseTree; // the generated parse tree
    private decafParser grammarParser; // the generated parser
    private decafLexer grammarLexer; // the generated lexer
    private static final String endOfLine = "<br/>"; // EOF for tree visualization
    private int level = 0; // tree begin index level for tree visualization
    private String prettyFileTree; // a pretty tree visualization in text

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        /* horizontal layout for input and console */
        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setSpacing(false);
        hLayout.setMargin(false);
        hLayout.setSizeFull();

        Label editorLbl = new Label("<strong>Editor de Texto</strong>", ContentMode.HTML);

        /* layout for the input area */
        final VerticalLayout layout = new VerticalLayout();
        final VerticalLayout consolePanelLayout = new VerticalLayout();
        consolePanelLayout.setSpacing(false);

        final VerticalLayout consoleLayout = new VerticalLayout();

        /* console panel */
        Panel consolePanel = new Panel("Consola");
        consolePanel.setHeight(500.0f, Unit.PIXELS);

        /* the actual input area */
        TextArea editor = new TextArea();
        editor.setSizeFull();
        editor.setWidth(100.0f, Unit.PERCENTAGE);
        editor.setHeight(420.0f, Unit.PIXELS);
        editor.addValueChangeListener(event -> {
           editorInput = String.valueOf(event.getValue());
        });

        /* Buttons for compilation, tree and to clear the console */
        Button button = new Button("Compilar Codigo");
        Button generateTreeBtn = new Button("Representacion Arbol");
        Button clrConsoleBtn = new Button("Limpiar Consola");

        button.setSizeFull();
        generateTreeBtn.setSizeFull();
        clrConsoleBtn.setSizeFull();

        generateTreeBtn.setEnabled(false);

        /* button listeners */
        button.addClickListener(e -> {
            if (editorInput != null) {
                /* clear the console */
                consolePanelLayout.removeAllComponents();

                /* generate a stream from input and create tree */
                CharStream charStream = CharStreams.fromString(editorInput);
                grammarLexer = new decafLexer(charStream);
                grammarLexer.removeErrorListeners();
                grammarLexer.addErrorListener(new CustomErrorListener(consolePanelLayout));
                CommonTokenStream commonTokenStream = new CommonTokenStream(grammarLexer);
                grammarParser = new decafParser(commonTokenStream);
                grammarParser.removeErrorListeners();
                grammarParser.addErrorListener(new CustomErrorListener(consolePanelLayout));

                grammarParseTree = grammarParser.program();
                Label lbl1 = new Label("<strong>TREE>> </strong>" + grammarParseTree.toStringTree(grammarParser)
                        + "<br><i>Para un arbol mas limpio, Presiona \"el boton  \" representacion de arbol.<i>", ContentMode.HTML);
                lbl1.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);
                //consolePanelLayout.addComponent(lbl1);

                // SEMANTIC CONTROL ------------------------------------------------------------------------------------
                ParseTreeWalker walker = new ParseTreeWalker();
                SemanticListener semanticListener = new SemanticListener(grammarParser);
                walker.walk(semanticListener, grammarParseTree);
                List<String> errList = semanticListener.getSemanticErrorsList();

                if (!errList.isEmpty()) {
                    for (String error : errList) {
                        Label errLbl = new Label("<strong>ERROR>> </strong>" + error, ContentMode.HTML);
                        lbl1.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);
                        consolePanelLayout.addComponent(errLbl);
                    }
                    Notification notification = new Notification("Compilado con errores", "Ver consola para mas detalles",
                            Notification.Type.ERROR_MESSAGE, true);
                    notification.setDelayMsec(4000);
                    notification.setPosition(Position.BOTTOM_RIGHT);
                    notification.show(Page.getCurrent());
                }

                Notification notification = new Notification("Compilacion lista!", "Ejecucion terminada!");
                notification.setDelayMsec(2000);
                notification.setPosition(Position.TOP_CENTER);
                notification.show(Page.getCurrent());

                generateTreeBtn.setEnabled(true);
            } else {
                Notification notification = new Notification("Codigo vacio ._.", "El editor esta vacio",
                        Notification.Type.WARNING_MESSAGE, true);
                notification.setDelayMsec(4000);
                notification.setPosition(Position.BOTTOM_RIGHT);
                notification.show(Page.getCurrent());
            }

        });

        /* visual tree representation */
        generateTreeBtn.addClickListener(event -> {
            TreeViewer viewer = new TreeViewer(Arrays.asList(grammarParser.getRuleNames()), grammarParseTree);
            viewer.setBorderColor(Color.WHITE);
            viewer.setBoxColor(Color.WHITE);
            try {
                viewer.save("tree.jpg");
                //generate indented tree
                List<String> ruleNamesList = Arrays.asList(grammarParser.getRuleNames());
                //System.out.println(ruleNamesList.toString());
                //System.out.println(prettyTree(parseTree, ruleNamesList));
                prettyFileTree = prettyTree(grammarParseTree, ruleNamesList);

                final Window window = new Window("Parse Tree");
                //window.setWidth(90.0f, Unit.PERCENTAGE);
                window.setHeight(90.0f, Unit.PERCENTAGE);
                window.center();
                window.setResizable(false);

                /* save the tree image */
                //String basepath = VaadinService.getCurrent().getBaseDirectory().getAbsolutePath();
                FileResource resource = new FileResource(new File("tree.jpg"));
                Image image = new Image("Arbol almacenado en  /tree.jpg", resource);

                HorizontalLayout treeLayout = new HorizontalLayout();
                treeLayout.setMargin(true);
                treeLayout.setSpacing(true);
                treeLayout.setHeight(100.0f, Unit.PERCENTAGE);
                treeLayout.addComponent(image);
                treeLayout.setComponentAlignment(image, Alignment.MIDDLE_CENTER);

                //add Panel to layout
                Panel fileTreePanel = new Panel();
                fileTreePanel.setHeight(500.0f, Unit.PIXELS);

                Label fileTreeLbl = new Label(prettyFileTree, ContentMode.HTML);
                fileTreeLbl.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);

                fileTreePanel.setContent(fileTreeLbl);
                treeLayout.addComponent(fileTreePanel);
                treeLayout.setComponentAlignment(fileTreePanel, Alignment.MIDDLE_LEFT);

                window.setContent(treeLayout);

                hLayout.getUI().getUI().addWindow(window);

                Notification notification = new Notification("Representacion visual lista!", "Presiona para cerrar");
                notification.setDelayMsec(500);
                notification.setPosition(Position.TOP_CENTER);
                notification.show(Page.getCurrent());

                generateTreeBtn.setEnabled(false);
            } catch (IOException e1) {
                e1.printStackTrace();
                Notification notification = new Notification("Problema al visualizar el arbol", e1.getMessage(),
                        Notification.Type.ERROR_MESSAGE, true);
                notification.setDelayMsec(4000);
                notification.setPosition(Position.BOTTOM_RIGHT);
                notification.show(Page.getCurrent());
            } catch (PrintException e1) {
                e1.printStackTrace();
                Notification notification = new Notification("Problema en la visualizacion del arbol", e1.getMessage(),
                        Notification.Type.ERROR_MESSAGE, true);
                notification.setDelayMsec(4000);
                notification.setPosition(Position.BOTTOM_RIGHT);
                notification.show(Page.getCurrent());
            }
        });

        clrConsoleBtn.addClickListener(event -> {
            consolePanelLayout.removeAllComponents();
        });

        Label mainlbl = new Label("<div style=\"font-size: 1.8em;\"><center><strong>VaaDecaf</strong></center><div>", ContentMode.HTML);
        mainlbl.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);

        Label subtitle = new Label("<center>IntelliJavier IDEA - Implementacion de DECAF con Java, ANTLR4 y el framework de Vaadin</center>", ContentMode.HTML);
        subtitle.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);

        VerticalLayout pagelayout = new VerticalLayout();
        pagelayout.addComponents(mainlbl, subtitle, hLayout);

        HorizontalLayout editorButtonsLayout = new HorizontalLayout();
        editorButtonsLayout.setSizeFull();
        editorButtonsLayout.addComponents(button, generateTreeBtn, clrConsoleBtn);

        consolePanel.setContent(consolePanelLayout);
        consoleLayout.addComponent(consolePanel);
        layout.addComponents(editorLbl, editor, editorButtonsLayout);
        hLayout.addComponents(layout, consoleLayout);
        
        setContent(pagelayout);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }

    /**
     * Generates an indented string tree
     * Based on: https://github.com/grosenberg/SnippetsTest/blob/master/src/test/java/net/certiv/remark/test/TestBase.java
     * @param tree
     * @param rules
     * @return
     */
    public String prettyTree(final Tree tree, final List<String> rules) {
        level = 0;
        return process(tree, rules).replaceAll("(?m)^\\s+$", "").replaceAll("\\r?\\n\\r?\\n", endOfLine);
    }

    /**
     * The actual process to generate the indented tree
     * Based on: https://github.com/grosenberg/SnippetsTest/blob/master/src/test/java/net/certiv/remark/test/TestBase.java
     * @param tree
     * @param rules
     * @return
     */
    private String process(final Tree tree, final List<String> rules) {
        if (tree.getChildCount() == 0) return Utils.escapeWhitespace(Trees.getNodeText(tree, rules), false);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(lead(level));
        level++;
        String n = Utils.escapeWhitespace(Trees.getNodeText(tree, rules), false);
        stringBuilder.append(n + ' ');
        for (int i = 0; i < tree.getChildCount(); i++) {
            stringBuilder.append(process(tree.getChild(i), rules));
        }
        level--;
        stringBuilder.append(lead(level));
        return stringBuilder.toString();
    }

    /**
     * Adds indentations
     * Based on: https://github.com/grosenberg/SnippetsTest/blob/master/src/test/java/net/certiv/remark/test/TestBase.java
     * @param level
     * @return
     */
    private String lead(int level) {
        StringBuilder sb = new StringBuilder();
        if (level > 0) {
            sb.append(endOfLine);
            for (int cnt = 0; cnt < level; cnt++) {
                sb.append("-");
            }
        }
        return sb.toString();
    }
}
