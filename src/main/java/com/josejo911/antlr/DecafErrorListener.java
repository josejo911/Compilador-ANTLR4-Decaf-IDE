package com.josejo911.antlr;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.Collections;
import java.util.List;

public class DecafErrorListener extends BaseErrorListener {

    public void SyntaxError(Recognizer<?,?> recognizer,
                            Object os,
                            int line, int pos,
                            String msg,
                            RecognitionException e) {
        List<String> stack = ((Parser)recognizer).getRuleInvocationStack();
        Collections.reverse(stack);
        System.err.println("rule stack: "+stack);
        System.err.println("line "+line+":"+pos+" at "+
                os+": "+msg);
    }
}
