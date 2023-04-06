package jobshop.encodings;

import jobshop.Instance;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;

/** Encoding of a solution by the ordering of tasks on each machine. */
public final class ResourceOrder extends Encoding {

    // for each machine m, taskByMachine[m] is an array of tasks to be
    // executed on this machine in the same order
    final Task[][] tasksByMachine;

    // for each machine, indicate how many tasks have been initialized
    final int[] nextFreeSlot;

    /** Creates a new empty resource order. */
    public ResourceOrder(Instance instance)
    {
        super(instance);

        // matrix of null elements (null is the default value of objects)
        tasksByMachine = new Task[instance.numMachines][instance.numJobs];

        // no task scheduled on any machine (0 is the default value)
        nextFreeSlot = new int[instance.numMachines];
    }

    /** Creates a resource order from a schedule. */
    public ResourceOrder(Schedule schedule)
    {
        super(schedule.instance);
        Instance pb = schedule.instance;

        this.tasksByMachine = new Task[pb.numMachines][];
        this.nextFreeSlot = new int[instance.numMachines];

        for(int m = 0; m<schedule.instance.numMachines ; m++) {
            final int machine = m;

            // for this machine, find all tasks that are executed on it and sort them by their start time
            tasksByMachine[m] =
                    IntStream.range(0, pb.numJobs) // all job numbers
                            .mapToObj(j -> new Task(j, pb.task_with_machine(j, machine))) // all tasks on this machine (one per job)
                            .sorted(Comparator.comparing(t -> schedule.startTime(t.job, t.task))) // sorted by start time
                            .toArray(Task[]::new); // as new array and store in tasksByMachine

            // indicate that all tasks have been initialized for machine m
            nextFreeSlot[m] = instance.numJobs;
        }
    }

    public ResourceOrder(ResourceOrder original) {
        super(original.instance);
        this.nextFreeSlot = original.nextFreeSlot.clone();
        this.tasksByMachine = new Task[original.tasksByMachine.length][];
        for(int i=0 ; i<this.tasksByMachine.length ; i++) {
            this.tasksByMachine[i] = original.tasksByMachine[i].clone();
        }
    }

    public Task getTaskFromMatrix(int machine,int indice){return this.tasksByMachine[machine][indice];}

    public int getIndexOnMachine(Task t,int machine){
        int index = -1;
        for(int i = 0;i<this.instance.numJobs;i++){
            if(t.equals(this.tasksByMachine[machine][i])){
                index = i;
                break;
            }
        }
        if(index == -1){
            System.out.println("LA tâche n'a pas été trouvée dans la matrice");
        }
        return index;
    }

    public void setTaskinMatrix(int machine,int indice,Task t){
        this.tasksByMachine[machine][indice] = t;
    }

    /** Adds the given task to the queue of the given machine. */
    public void addTaskToMachine(int machine, Task task) {
        if(instance.machine(task) != machine) {
            throw new RuntimeException("Task " + task + " cannot be scheduled on machine "+machine);
        }
        tasksByMachine[machine][nextFreeSlot[machine]] = task;
        nextFreeSlot[machine] += 1;
    }

    /** Returns the i-th task scheduled on a particular machine.
     *
     * @param machine Machine on which the task to retrieve is scheduled.
     * @param taskIndex Index of the task in the queue for this machine.
     * @return The i-th task scheduled on a machine.
     */
    public Task getTaskOfMachine(int machine, int taskIndex) {
        return tasksByMachine[machine][taskIndex];
    }

    /** Exchange the order of two tasks that are scheduled on a given machine.
     *
     * @param machine Machine on which the two tasks appear (line on which to perform the exchange)
     * @param indexTask1 Position of the first task in the machine's queue
     * @param indexTask2 Position of the second task in the machine's queue
     */
    public void swapTasks(int machine, int indexTask1, int indexTask2) {
        Task tmp = tasksByMachine[machine][indexTask1];
        tasksByMachine[machine][indexTask1] = tasksByMachine[machine][indexTask2];
        tasksByMachine[machine][indexTask2] = tmp;
    }

    @Override
    public Optional<Schedule> toSchedule() {
        // indicates, for each task that have been scheduled, its start time
        Schedule schedule = new Schedule(instance);

        // for each job, how many tasks have been scheduled (0 initially)
        int[] nextToScheduleByJob = new int[instance.numJobs];

        // for each machine, how many tasks have been scheduled (0 initially)
        int[] nextToScheduleByMachine = new int[instance.numMachines];

        // for each machine, earliest time at which the machine can be used
        int[] releaseTimeOfMachine = new int[instance.numMachines];


        // loop while there remains a job that has unscheduled tasks
        while(IntStream.range(0, instance.numJobs).anyMatch(m -> nextToScheduleByJob[m] < instance.numTasks)) {

            // selects a task that has no unscheduled predecessor on its job and machine :
            //  - it is the next to be schedule on a machine
            //  - it is the next to be scheduled on its job
            // If there is no such task, we have cyclic dependency and the solution is invalid.
            Optional<Task> schedulable =
                    IntStream.range(0, instance.numMachines) // all machines ...
                    .filter(m -> nextToScheduleByMachine[m] < instance.numJobs) // ... with unscheduled jobs
                    .mapToObj(m -> this.tasksByMachine[m][nextToScheduleByMachine[m]]) // tasks that are next to schedule on a machine ...
                    .filter(task -> task.task == nextToScheduleByJob[task.job])  // ... and on their job
                    .findFirst(); // select the first one if any

            if(schedulable.isPresent()) {
                // we have a schedulable task, lets call it t
                Task t = schedulable.get();
                int machine = instance.machine(t.job, t.task);

                // compute the earliest start time (est) of the task
                int est = t.task == 0 ? 0 : schedule.endTime(t.job, t.task-1);
                est = Math.max(est, releaseTimeOfMachine[instance.machine(t)]);
                schedule.setStartTime(t.job, t.task, est);

                // mark the task as scheduled
                nextToScheduleByJob[t.job]++;
                nextToScheduleByMachine[machine]++;
                // increase the release time of the machine
                releaseTimeOfMachine[machine] = est + instance.duration(t.job, t.task);
            } else {
                // no tasks are schedulable, there is no solution for this resource ordering
                return Optional.empty();
            }
        }
        // we exited the loop : all tasks have been scheduled successfully
        return Optional.of(schedule);
    }

    /** Creates an exact copy of this resource order.
     *
     * May fail if the resource order does not represent a valid solution.
     */
    public ResourceOrder copy() {
        return new ResourceOrder(this);
    }

    @Override
    protected Object clone() {
        return new ResourceOrder(this);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for(int m=0; m < instance.numMachines; m++)
        {
            s.append("Machine ").append(m).append(" : ");
            for(int j=0; j<instance.numJobs; j++)
            {
                s.append(tasksByMachine[m][j]).append(" ; ");
            }
            s.append("\n");
        }

        return s.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceOrder that = (ResourceOrder) o;
        return Arrays.deepEquals(tasksByMachine, that.tasksByMachine) && Arrays.equals(nextFreeSlot, that.nextFreeSlot);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(tasksByMachine);
        result = 31 * result + Arrays.hashCode(nextFreeSlot);
        return result;
    }
}