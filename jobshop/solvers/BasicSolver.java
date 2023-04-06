package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;

import java.util.Optional;

/**
 * A very naïve solver that first schedules all first tasks, then all second tasks, ...
 **/
public class BasicSolver implements Solver {
    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {

        // resource order that will be populated (initially empty)
        ResourceOrder sol = new ResourceOrder(instance);

        // in the resource order:
        // - enqueue all first tasks, then
        // - enqueue all second tasks, then
        // ...
        for(int taskNumber = 0 ; taskNumber<instance.numTasks ; taskNumber++) {
            for(int jobNumber = 0 ; jobNumber<instance.numJobs ; jobNumber++) {
                Task taskToEnqueue = new Task(jobNumber, taskNumber);
                sol.addTaskToMachine(instance.machine(jobNumber, taskNumber), taskToEnqueue);
            }
        }

        // Convert the resource order into a schedule and return it
        return sol.toSchedule();
    }
}
