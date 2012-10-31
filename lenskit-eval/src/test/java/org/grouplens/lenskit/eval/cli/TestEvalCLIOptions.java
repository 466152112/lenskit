/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2012 Regents of the University of Minnesota and contributors
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

import static org.grouplens.lenskit.eval.cli.EvalCLIOptions.parse;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import org.grouplens.lenskit.eval.config.EvalConfig;
import org.junit.Test;

import java.io.File;

/**
 * Test the eval CLI option parser.
 * @author Michael Ekstrand
 */
public class TestEvalCLIOptions {
    @Test
    public void testNoArguments() {
        String[] args = {};
        EvalCLIOptions opts = parse(args);
        // properties aren't null
        assertThat(opts.getProperties(),
                   notNullValue());
        // args is empty array
        assertThat(opts.getArgs(), notNullValue());
        assertThat(opts.getArgs().length, equalTo(0));
        // config file is proper default
        assertThat(opts.getScriptFile(),
                   equalTo(new File("eval.groovy")));

        EvalConfig cfg = new EvalConfig(opts.getProperties());
        assertThat(cfg.force(), equalTo(false));
        assertThat(cfg.getThreadCount(), equalTo(1));
    }

    @Test
    public void testForce() {
        String[] args = {"--force"};
        EvalCLIOptions opts = parse(args);
        EvalConfig cfg = new EvalConfig(opts.getProperties());
        assertThat(cfg.force(), equalTo(true));
    }

    @Test
    public void testScript() {
        String[] args = {"-ffoo.groovy"};
        EvalCLIOptions opts = parse(args);
        assertThat(opts.getScriptFile(), equalTo(new File("foo.groovy")));
    }

    @Test
    public void testDefine() {
        String[] args = {"-Dscroll.name=hackem muche"};
        EvalCLIOptions opts = parse(args);
        assertThat(opts.getProperties().getProperty("scroll.name"),
                   equalTo("hackem muche"));
        EvalConfig cfg = new EvalConfig(opts.getProperties());
        assertThat(cfg.get("scroll.name"), equalTo("hackem muche"));
    }

    @Test
    public void testThreadCount() {
        String[] args = {"-j4"};
        EvalCLIOptions opts = parse(args);
        EvalConfig cfg = new EvalConfig(opts.getProperties());
        assertThat(cfg.getThreadCount(), equalTo(4));
    }

    @Test
    public void testAutoThreadCount() {
        String[] args = {"-j"};
        EvalCLIOptions opts = parse(args);
        EvalConfig cfg = new EvalConfig(opts.getProperties());
        assertThat(cfg.getThreadCount(),
                   equalTo(Runtime.getRuntime().availableProcessors()));
    }

    @Test
    public void testMultiThreadCount() {
        String[] args = {"-j", "-F"};
        EvalCLIOptions opts = parse(args);
        EvalConfig cfg = new EvalConfig(opts.getProperties());
        assertThat(cfg.getThreadCount(),
                   equalTo(Runtime.getRuntime().availableProcessors()));
        assertThat(cfg.force(), equalTo(true));
    }
}
