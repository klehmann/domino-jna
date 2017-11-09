package com.mindoo.domino.jna.utils;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Implementation of a {@link StringTokenizer} that splits up a string into several parts
 * based on a delimiter. In contrast to the JDK's tokenizer, the {@link StringTokenizerExt}
 * supports empty parts (e.g. "word1,,word2").
 * <br>
 * It only throws an exception of all available tokens have already been read.
 */
public class StringTokenizerExt {
    private String s;
    private String delim;
    private int pos = 0;
    
    /**
     * Creates a new instance
     * 
     * @param s string to be tokenized
     * @param delim delimier
     */
    public StringTokenizerExt(String s, String delim) {
        this.s = s;
        this.delim = delim;
        
        if ("".equals(s))
            this.pos = -1;
        else
            this.pos = 0;
    }
    
    /**
     * Method to check if there are more tokens
     * 
     * @return true if there are tokens to read
     */
    public boolean hasMoreTokens() {
        return (this.pos != -1);
    }
    
    /**
     * Returns the next token
     * 
     * @return Token
     * @throws NoSuchElementException , if there are no more tokens left
     */
    public String nextToken() throws NoSuchElementException {
        if (this.pos == -1)
            throw new NoSuchElementException();
        
        int nextDelimPos = this.s.indexOf(this.delim, this.pos);
        
        if (nextDelimPos == -1) {
            //return rest after last delimiter
            String retVal = this.s.substring(this.pos);
            this.pos = -1;
            return retVal;
        }
        else {
            //text until next delimiter
            String retVal = this.s.substring(this.pos, nextDelimPos);
            this.pos = nextDelimPos+(this.delim.length());
            
            return retVal;
        }
        
    }
}