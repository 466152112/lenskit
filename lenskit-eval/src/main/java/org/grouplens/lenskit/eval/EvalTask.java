package org.grouplens.lenskit.eval;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by IntelliJ IDEA.
 * User: schang
 * Date: 3/14/12
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
public interface EvalTask<T> extends Callable {

    /**
     * Get a descriptive name for this job.  The name is displayed in UI to let
     * the user know what is being run.  More specific descriptors identifying
     * this job to allow its output to be processed should be output directly
     * to the output handler when the job is run.
     *
     * @return The name for this job.
     */
    String getName();


    Set<EvalTask> getDependency();

    @Override
    T call() throws Exception;

}
