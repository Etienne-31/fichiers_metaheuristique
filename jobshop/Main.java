package jobshop;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jobshop.encodings.Schedule;
import jobshop.solvers.*;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * This class is the main entry point for doing comparative performance tests of solvers.
 */
public class Main {

    public static void main(String[] args) {
        // configure the argument parser
        ArgumentParser parser = ArgumentParsers.newFor("jsp-solver").build()
                .defaultHelp(true)
                .description("Solves jobshop problems.");
        parser.addArgument("-t", "--timeout")
                .setDefault(1L)
                .type(Long.class)
                .help("Solver timeout in seconds for each instance. Default is 1 second.");
        parser.addArgument("--solver")
                .nargs("+")
                .required(true)
                .help("Solver(s) to use (space separated if more than one)");
        parser.addArgument("--instance")
                .nargs("+")
                .required(true)
                .help("Instance(s) to solve (space separated if more than one). All instances starting with the given " +
                        "string will be selected. (e.g. \"ft\" will select the instances ft06, ft10 and ft20.");

        // parse command line arguments
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            // error while parsing arguments, provide helpful error message and exit.
            System.err.println("Invalid arguments provided to the program.\n");
            System.err.println("In IntelliJ, you can provide arguments to the program by opening the dialog,");
            System.err.println("\"Run > Edit Configurations\" and filling in the \"program arguments\" box.");
            System.err.println("See the README for a documentation of the expected arguments.");
            System.err.println();
            parser.handleError(e);
            System.exit(0);
        }

        PrintStream output = System.out;

        // convert the timeout from seconds to milliseconds.
        long solveTimeMs = ns.getLong("timeout") * 1000;

        // Get the list of solvers that we should benchmark.
        // We also check that we have a solver available for the given name and print an error message otherwise.
        List<String> solversToTest = ns.getList("solver");
        List<Solver> solvers = solversToTest.stream().map(Solver::getSolver).collect(Collectors.toList());

        // retrieve all instances on which we should run the solvers.
        List<String> instances = new ArrayList<>();
        List<String> instancePrefixes = ns.getList("instance");
        for(String instancePrefix : instancePrefixes) {
            List<String> matches = BestKnownResults.instancesMatching(instancePrefix);
            if(matches.isEmpty()) {
                System.err.println("ERROR: instance prefix \"" + instancePrefix + "\" does not match any instance.");
                System.err.println("       available instances: " + Arrays.toString(BestKnownResults.instances));
                System.exit(1);
            }
            instances.addAll(matches);
        }

        // average runtime of each solver
        float[] avg_runtimes = new float[solversToTest.size()];
        // average distance to best known result for each solver
        float[] avg_distances = new float[solversToTest.size()];

        try {
            // header of the result table :
            //   - solver names (first line)
            //   - name of each column (second line)
            output.print(  "                         ");
            for(String s : solversToTest)
                output.printf("%-30s", s);
            output.println();
            output.print("instance size  best      ");
            for(String s : solversToTest) {
                output.print("runtime makespan ecart        ");
            }
            output.println();

            // for all instances, load it from f
            for(String instanceName : instances) {
                // get the best known result for this instance
                int bestKnown = BestKnownResults.of(instanceName);

                // load instance from file.
                Path path = Paths.get("instances/", instanceName);
                Instance instance = Instance.fromFile(path);

                // print some general statistics on the instance
                output.printf("%-8s %-5s %4d      ",instanceName, instance.numJobs +"x"+instance.numTasks, bestKnown);

                // run all selected solvers on the instance and print the results
                for(int solverId = 0 ; solverId < solvers.size() ; solverId++) {
                    // Select the next solver to run. Given the solver name passed on the command line,
                    // we lookup the `Main.solvers` hash map to get the solver object with the given name.
                    Solver solver = solvers.get(solverId);

                    // start chronometer and compute deadline for the solver to provide a result.
                    long start = System.currentTimeMillis();
                    long deadline = System.currentTimeMillis() + solveTimeMs;
                    // run the solver on the current instance
                    Optional<Schedule> result = solver.solve(instance, deadline);
                    // measure elapsed time (in milliseconds)
                    long runtime = System.currentTimeMillis() - start;

                    // check that the solver returned a valid solution
                    if(result.isEmpty() || !result.get().isValid()) {
                        System.err.println("ERROR: solver did not provide a valid schedule");
                        System.exit(1); // bug in implementation, bail out
                    }
                    // we have a valid schedule
                    Schedule schedule = result.get();

                    // compute some statistics on the solution and print them.
                    int makespan = schedule.makespan();
                    float dist = 100f * (makespan - bestKnown) / (float) bestKnown;
                    avg_runtimes[solverId] += (float) runtime / (float) instances.size();
                    avg_distances[solverId] += dist / (float) instances.size();

                    output.printf("%7d %8s %5.1f        ", runtime, makespan, dist);
                    output.flush();
                }
                output.println();
            }


            // we have finished all benchmarks, compute the average solve time and distance of each solver.
            output.printf("%-8s %-5s %4s      ", "AVG", "-", "-");
            for(int solverId = 0 ; solverId < solversToTest.size() ; solverId++) {
                output.printf("%7.1f %8s %5.1f        ", avg_runtimes[solverId], "-", avg_distances[solverId]);
            }



        } catch (Exception e) {
            // there was uncaught exception, print the stack trace and exit with error.
            e.printStackTrace();
            System.exit(1);
        }
    }
}
