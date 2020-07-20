package com.josejo911.antlr;

import com.vaadin.server.Page;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Notification;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
/**
 * Author: Javier Jo
 * Version: 0.0.1
 * */

public class CustomErrorListener extends BaseErrorListener {
    //public static CustomErrorListener INSTANCE = new CustomErrorListener();
    Layout layout;

    public CustomErrorListener(Layout layout) {
        this.layout = layout;
    }

    @Override
    public void syntaxError(Recognizer<?,?> recognizer, Object offSymb, int line, int charPos, String msg, RecognitionException e) {
        String sourceName = recognizer.getInputStream().getSourceName();
        if (!sourceName.isEmpty()) {
            sourceName = String.format("%s:%d:%d: ", sourceName, line, charPos);
        }

        Label lbl = new Label("<strong>ERROR >> </strong> Line <strong>"+line+":"+charPos+ "</strong> "+msg, ContentMode.HTML);
        lbl.setWidth(100.0f, Sizeable.Unit.PERCENTAGE);
        layout.addComponent(lbl);

        Notification notification = new Notification("Compiled with errors", "See console for details",
                Notification.Type.ERROR_MESSAGE, true);
        notification.setDelayMsec(4000);
        notification.setPosition(Position.BOTTOM_RIGHT);
        notification.show(Page.getCurrent());

        System.out.println(sourceName+"line "+line+":"+charPos+ " "+msg);
    }
}
