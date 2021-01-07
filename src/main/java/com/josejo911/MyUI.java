package com.josejo911;

import javax.print.PrintException;
import javax.servlet.annotation.WebServlet;

import com.josejo911.antlr.CustomErrorListener;
import com.josejo911.antlrtac.tacLexer;
import com.josejo911.antlrtac.tacParser;
import com.josejo911.semanticControl.SemanticListener;
import com.josejo911.antlr.decafLexer;
import com.josejo911.antlr.decafParser;
import com.josejo911.semanticControl.TacSemanticListener;
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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * IDE UI for the Decaf Compiler
 * @author Jose Jo
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

        Label editorLbl = new Label("<strong>Editor de Codigo </strong>", ContentMode.HTML);

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
        Button button = new Button("Compilar");
        Button generateTreeBtn = new Button("Arbol");
        Button clrConsoleBtn = new Button("Limpiar Consola");
        Button showTAC = new Button("TAC");
        Button showMIPS = new Button("MIPS");

        button.setSizeFull();
        generateTreeBtn.setSizeFull();
        clrConsoleBtn.setSizeFull();
        showTAC.setSizeFull();
        showMIPS.setSizeFull();

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
                        + "<br><i>Para una vista mas clara del arbol, presionar \"Tree representation \" boton.<i>", ContentMode.HTML);
                lbl1.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);
                //consolePanelLayout.addComponent(lbl1);

                // SEMANTIC CONTROL ------------------------------------------------------------------------------------
                /* TAC FILE GENERATION */
                generateTACFile();

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
                    Notification notification = new Notification("Compilado con errores", "Ver la consola para mas detalles",
                            Notification.Type.ERROR_MESSAGE, true);
                    notification.setDelayMsec(4000);
                    notification.setPosition(Position.BOTTOM_RIGHT);
                    notification.show(Page.getCurrent());
                }

                Notification notification = new Notification("Compilacion terminada!", "Ejecucion terminada!");
                notification.setDelayMsec(2000);
                notification.setPosition(Position.TOP_CENTER);
                notification.show(Page.getCurrent());

                generateTreeBtn.setEnabled(true);

                /* Tac Parser generation and walkthrough */
                Scanner in = null;
                String tac = "";
                try {
                    in = new Scanner(new FileReader("decaf.tac"));
                    StringBuilder sb = new StringBuilder();
                    while(in.hasNext()) {
                        sb.append(in.nextLine() + "\n");
                    }
                    in.close();
                    tac = sb.toString();
                } catch (FileNotFoundException er) {
                    er.printStackTrace();
                }

                CharStream cs = CharStreams.fromString(tac);
                tacLexer tl = new tacLexer(cs);
                CommonTokenStream cts = new CommonTokenStream(tl);
                tacParser tp = new tacParser(cts);

                ParseTree pt = tp.program();

                ParseTreeWalker tacTreeWalker = new ParseTreeWalker();
                TacSemanticListener tacSL = new TacSemanticListener(tp, semanticListener.getVarList(), semanticListener.getMethodFirms());
                walker.walk(tacSL, pt);


            } else {
                Notification notification = new Notification("Empty code", "The editor is empty",
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
                Image image = new Image("Arbol guardado en el archivo /tree.jpg", resource);

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

                Notification notification = new Notification("Representacion visual terminada!", "Presionar para oculatar");
                notification.setDelayMsec(500);
                notification.setPosition(Position.TOP_CENTER);
                notification.show(Page.getCurrent());

                generateTreeBtn.setEnabled(false);
            } catch (IOException e1) {
                e1.printStackTrace();
                Notification notification = new Notification("Error en la visualizacion del arbol ", e1.getMessage(),
                        Notification.Type.ERROR_MESSAGE, true);
                notification.setDelayMsec(4000);
                notification.setPosition(Position.BOTTOM_RIGHT);
                notification.show(Page.getCurrent());
            } catch (PrintException e1) {
                e1.printStackTrace();
                Notification notification = new Notification("Error en la visualizacion del arbol ", e1.getMessage(),
                        Notification.Type.ERROR_MESSAGE, true);
                notification.setDelayMsec(4000);
                notification.setPosition(Position.BOTTOM_RIGHT);
                notification.show(Page.getCurrent());
            }
        });

        clrConsoleBtn.addClickListener(event -> {
            consolePanelLayout.removeAllComponents();
        });

        showTAC.addClickListener(event -> {
            final Window window = new Window("TAC Generado");
            //window.setWidth(90.0f, Unit.PERCENTAGE);
            window.setHeight(90.0f, Unit.PERCENTAGE);
            window.setWidth(50.0f, Sizeable.Unit.PERCENTAGE);
            window.center();
            window.setResizable(false);

            Scanner in = null;
            String outString = "";
            try {
                in = new Scanner(new FileReader("decaf.tac"));
                StringBuilder sb = new StringBuilder();
                while(in.hasNext()) {
                    sb.append(in.nextLine() + "\n");
                }
                in.close();
                outString = sb.toString();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            Panel tacPanel = new Panel();

            Label tacLabel = new Label(outString.replace("<", " < ")
                    .replace("\n", "<br>")
                    .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"), ContentMode.HTML);

            VerticalLayout tacLayout = new VerticalLayout();
            tacLayout.setSpacing(true);
            tacLayout.addComponent(tacLabel);

            tacPanel.setContent(tacLayout);

            window.setContent(tacPanel);

            hLayout.getUI().getUI().addWindow(window);
        });

        showMIPS.addClickListener(event -> {
            final Window window = new Window("MIPS Generado");
            //window.setWidth(90.0f, Unit.PERCENTAGE);
            window.setHeight(90.0f, Unit.PERCENTAGE);
            window.setWidth(50.0f, Sizeable.Unit.PERCENTAGE);
            window.center();
            window.setResizable(false);

            Scanner in = null;
            String outString = "";
            try {
                in = new Scanner(new FileReader("generated_mips.asm"));
                StringBuilder sb = new StringBuilder();
                while(in.hasNext()) {
                    sb.append(in.nextLine() + "\n");
                }
                in.close();
                outString = sb.toString();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            Panel tacPanel = new Panel();

            Label tacLabel = new Label(outString.replace("<", " < ")
                    .replace("\n", "<br>")
                    .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"), ContentMode.HTML);

            VerticalLayout tacLayout = new VerticalLayout();
            tacLayout.setSpacing(true);
            tacLayout.addComponent(tacLabel);

            tacPanel.setContent(tacLayout);

            window.setContent(tacPanel);

            hLayout.getUI().getUI().addWindow(window);
        });

        Label mainlbl = new Label("<div style=\"font-size: 1.8em;\"><center><strong>IntelliJO IDEA</strong></center><div>", ContentMode.HTML);
        mainlbl.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);

        Label subtitle = new Label("<center>Implementacion de DECAF con Java, ANTLR4 y el framework de VAADIN</center>", ContentMode.HTML);
        subtitle.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);

        VerticalLayout pagelayout = new VerticalLayout();
        pagelayout.addComponents(mainlbl, subtitle, hLayout);

        HorizontalLayout editorButtonsLayout = new HorizontalLayout();
        editorButtonsLayout.setSizeFull();
        editorButtonsLayout.addComponents(button, generateTreeBtn, showTAC, showMIPS, clrConsoleBtn);

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

    private void generateTACFile() {
        try {
            File file = new File("decaf.tac");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File file = new File("generated_mips.asm");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
