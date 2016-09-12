/*
 * 
 * Copyright 2014, Armenak Grigoryan, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package com.strider.datadefender.functions;

import com.strider.datadefender.utils.Xeger;
import com.strider.datadefender.utils.XegerTest;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import static org.apache.log4j.Logger.getLogger;

/**
 * Core data anonymizer functions
 * 
 * @author Armenak Grigoryan
 */
public class CoreFunctionsTest extends TestCase {
    
    private static Logger log = getLogger(XegerTest.class);
    
    private final static String regExpPattern = "[0-9]{3}-[0-9]{3}-[0-9]{3}";
    
    public CoreFunctionsTest(final String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of generateStringFromPattern method, of class CoreFunctions.
     */
    public void testGenerateStringFromPattern() {
        log.debug("Generate SIN");
        final Xeger instance = new Xeger(regExpPattern);
        final String text = instance.generate();
        log.debug(text);
        assertTrue(text.matches(regExpPattern));
    }
    
    public void testLipsumParagraphs() throws Exception {
        final CoreFunctions cf = new CoreFunctions();
        final String paragraphs = cf.lipsumParagraphs(3);
        log.debug("Testing for 3 paragraphs");
        assertTrue(paragraphs.matches("^[^\r]+\r\n\r\n[^\r]+\r\n\r\n[^\r]+$"));
    }
    
    public void testLipsumSentences() throws Exception {
        final CoreFunctions cf = new CoreFunctions();
        final String sentences = cf.lipsumSentences(3, 3);
        log.debug("Testing for 3 sentences");
        assertTrue(sentences.matches("([^\\.]+\\.){3}"));
    }
    
    public void testLipsumSimilar() throws Exception {
        final CoreFunctions cf = new CoreFunctions();
        final String sentences = cf.lipsumSimilar("This is a test.  It is excellent.  Should have a minimum of two sentences.");
        log.debug("Testing for 3 sentences generated by similar text");
        assertTrue(sentences.matches("([^\\.]+\\.){2,}"));
        final String paras = cf.lipsumSimilar("This is also a test.\n\nIt is better.\n\nShould have three paragraphs.");
        log.debug("Testing for 3 paragraphs generated by text with 3 paragraphs");
        assertTrue(paras.matches("^[^\r]+\r\n\r\n[^\r]+\r\n\r\n[^\r]+$"));
    }
}
