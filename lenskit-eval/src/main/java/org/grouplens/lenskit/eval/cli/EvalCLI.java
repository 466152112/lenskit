/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2011 Regents of the University of Minnesota
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.grouplens.lenskit.eval.cli;

import org.grouplens.lenskit.eval.EvalRunner;
import org.grouplens.lenskit.eval.EvaluatorConfigurationException;
import org.grouplens.lenskit.eval.PreparationContext;
import org.grouplens.lenskit.eval.PreparationException;
import org.grouplens.lenskit.util.dtree.DataNode;
import org.grouplens.lenskit.util.dtree.Trees;
import org.grouplens.lenskit.util.dtree.xml.XMLDataNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Main entry point to run the evaluator from the command line
 * 
 * @since 0.8
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class EvalCLI {
    private static final Logger logger = LoggerFactory.getLogger(EvalCLI.class);
    
    /**
     * Run the evaluator from the command line.
     * @param args The command line arguments to the evaluator.
     */
    public static void main(String[] args) {
        EvalOptions options = EvalOptions.parse(args);
        EvalCLI cli = new EvalCLI(options);
        cli.run();
    }
    
    EvalOptions options;
    
    public EvalCLI(EvalOptions opts) {
        options = opts;
    }
    
    public void run() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        
        List<EvalRunner> runners = new LinkedList<EvalRunner>();
        for (File f: options.getConfigFiles()) {
            Document doc;
            try {
                doc = builder.parse(f);
            } catch (SAXException e) {
                reportError(e, "%s: %s", f.getPath(), e.getMessage());
                return;
            } catch (IOException e) {
                reportError(e, "%s: %s\n", f.getPath(), e.getMessage());
                return;
            }
            
            DataNode node = XMLDataNode.wrap(options.getProperties(), doc);
            if (!node.getName().equals("evaluation")) {
                reportError("%s: root element must be 'evaluation'", f.getPath());
            }
            
            String name = Trees.childValue(node, "type");
            if (name == null) {
                reportError("%s: no evaluation type specified\n", f.getPath());
            }
            
            logger.info("Configuring evaluation type {} from {}", name, f);
            try {
                runners.add(new EvalRunner(name, System.getProperties(), node));
            } catch (EvaluatorConfigurationException e) {
                reportError(e, "%s: %s\n", f.getPath(), e.getMessage());
                return;
            }
        }
        
        PreparationContext context = new PreparationContext();
        context.setUnconditional(options.forcePrepare());
        context.setCacheDirectory(options.getCacheDir());
        for (EvalRunner runner: runners) {
            runner.setIsolationLevel(options.getIsolation());
            runner.setThreadCount(options.getThreadCount());
            try {
                runner.prepare(context);
            } catch (PreparationException e) {
                reportError(e, "Preparation error: " + e.getMessage());
                return;
            }
            
            if (!options.isPrepareOnly()) {
                try {
                    runner.run();
                } catch (ExecutionException e) {
                    reportError(e, "Error running evaluation: %s", e.getMessage());
                    return;
                }
            }
        }
    }
    
    protected void reportError(String msg, Object... args) {
        reportError(null, msg, args);
    }
    
    protected void reportError(Exception e, String msg, Object... args) {
        String text = String.format(msg, args);
        System.err.println(text);
        if (options.throwErrors()) {
            throw new RuntimeException(text, e);
        } else {
            if (e != null && options.printBacktraces())
                e.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
