package jobshop.encodings;

import jobshop.Instance;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Direct encoding of the solution to JobShop problem.
 *
 * Associates every task to its start time.
 */
public class Schedule extends Encoding {

    // start times of each job and task
    // times[j][i] is the start time of task (j,i) : i^th task of the j^th job
    final int[][] times;

    /** Creates a new schedule for the given instance where all start times are uninitialized. */
    public Schedule(Instance instance) {
        super(instance);
        this.times = new int[instance.numJobs][];
        for(int j = 0; j < instance.numJobs ; j++) {
            this.times[j] = new int[instance.numTasks];
        }
    }


    /** Start time of the given task. */
    public int startTime(int job, int task) {
        return times[job][task];
    }

    /** Start time of the given task. */
    public int startTime(Task task) {
        return startTime(task.job, task.task);
    }

    /** End time of the given task. */
    public int endTime(int job, int task) {
        return startTime(job, task) + instance.duration(job, task);
    }

    /** End time of the given task. */
    public int endTime(Task task) {
        return endTime(task.job, task.task);
    }

    /** Sets the start time of the given task. */
    public void setStartTime(int job, int task, int startTime) {
        times[job][task] = startTime;
    }

    /** Returns true if this schedule is valid (no constraint is violated) */
    public boolean isValid() {
        for(int j = 0; j<instance.numJobs ; j++) {
            for(int t = 1; t< instance.numTasks ; t++) {
                if(startTime(j, t-1) + instance.duration(j, t-1) > startTime(j, t))
                    return false;
            }
            for(int t = 0; t< instance.numTasks ; t++) {
                if(startTime(j, t) < 0)
                    return false;
            }
        }

        for (int machine = 0; machine < instance.numMachines ; machine++) {
            for(int j1 = 0; j1< instance.numJobs ; j1++) {
                int t1 = instance.task_with_machine(j1, machine);
                for(int j2 = j1+1; j2< instance.numJobs ; j2++) {
                    int t2 = instance.task_with_machine(j2, machine);

                    boolean t1_first = endTime(j1, t1) <= startTime(j2, t2);
                    boolean t2_first = endTime(j2, t2) <= startTime(j1, t1);

                    if(!t1_first && !t2_first)
                        return false;
                }
            }
        }

        return true;
    }

    /** Makespan of the solution.
     * The makespan is the end time of the latest finishing task.
     */
    public int makespan() {
        int max = -1;
        for(int j = 0; j< instance.numJobs ; j++) {
            max = Math.max(max, endTime(j, instance.numTasks-1));
        }
        return max;
    }

    /** Returns true if the given sequence of task is a critical path of the schedule. */
    public boolean isCriticalPath(List<Task> path) {
        if(startTime(path.get(0)) != 0) {
            return false;
        }
        if(endTime(path.get(path.size()-1)) != makespan()) {
            return false;
        }
        for(int i=0 ; i<path.size()-1 ; i++) {
            if(endTime(path.get(i)) != startTime(path.get(i+1)))
                return false;
        }
        return true;
    }

    /** Computes a critical path of the schedule.
     *
     * @return A sequence of task along a critical path.
     */
    public List<Task> criticalPath() {
        // select task with greatest end time
        Task ldd = IntStream.range(0, instance.numJobs)
                .mapToObj(j -> new Task(j, instance.numTasks-1))
                .max(Comparator.comparing(this::endTime))
                .get();
        assert endTime(ldd) == makespan();

        // list that will contain the critical path.
        // we construct it from the end, starting with the
        // task that finishes last
        LinkedList<Task> path = new LinkedList<>();
        path.add(0,ldd);

        // keep adding tasks to the path until the first task in the path
        // starts a time 0
        while(startTime(path.getFirst()) != 0) {
            Task cur = path.getFirst();
            int machine = instance.machine(cur.job, cur.task);

            // will contain the task that was delaying the start
            // of our current task
            Optional<Task> latestPredecessor = Optional.empty();

            if(cur.task > 0) {
                // our current task has a predecessor on the job
                Task predOnJob = new Task(cur.job, cur.task -1);

                // if it was the delaying task, save it to predecessor
                if(endTime(predOnJob) == startTime(cur))
                    latestPredecessor = Optional.of(predOnJob);
            }
            if(latestPredecessor.isEmpty()) {
                // no latest predecessor found yet, look among tasks executing on the same machine
                latestPredecessor = IntStream.range(0, instance.numJobs)
                        .mapToObj(j -> new Task(j, instance.task_with_machine(j, machine)))
                        .filter(t -> endTime(t) == startTime(cur))
                        .findFirst();
            }
            // at this point we should have identified a latest predecessor, either on the job or on the machine
            assert latestPredecessor.isPresent() && endTime(latestPredecessor.get()) == startTime(cur);
            // insert predecessor at the beginning of the path
            path.add(0, latestPredecessor.get());
        }
        assert isCriticalPath(path);
        return path;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nStart times of all tasks:\n");
        for(int job = 0; job< instance.numJobs; job++) {
            sb.append("Job ");
            sb.append(job);
            sb.append(": ");
            for(int task = 0; task< instance.numTasks; task++) {
                sb.append(String.format("%5d",  startTime(job, task)));

            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns a string containing the Gantt chart of the given schedule, in ASCII art.
     *
     * Each line of the Gantt chart, contains the tasks of a particular job. Each character in the output represents a
     * fixed number of time units
     * For each task, we indicate :
     *  - the machine on which the task must be executed
     *  - whether this task is on the critical path (task on the critical path are filled in with stars).
     */
    public String asciiGantt() {
        var criticalPath = this.criticalPath();
        int minTaskDur = IntStream.range(0, instance.numJobs).flatMap(job -> IntStream.range(0, instance.numTasks).map(task -> instance.duration(job, task))).min().getAsInt();
        // time units by character
        int charsPerTimeUnit = minTaskDur >= 5 ? 1 : (5 / minTaskDur) +1;
        StringBuilder sb = new StringBuilder();
        sb.append("\nGantt Chart\n");
        for(int job = 0; job< instance.numJobs; job++) {
            sb.append(String.format("Job %2d: ", job));
            int cursor = 0;
            for(int task = 0; task< instance.numTasks; task++) {
                Task t = new Task(job, task);
                var st = startTime(job, task);
                // add spaces until the start of our task
                sb.append(" ".repeat(charsPerTimeUnit * (st - cursor )));
                sb.append(formatTask(t, charsPerTimeUnit, criticalPath.contains(t)));

                cursor = endTime(job, task);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** Utility function to display a set of characters representing a task in a gantt chart.
     *
     * @param t Task to display
     * @param charPerTimeUnit How many characters to represent a time unit.
     * @param isCritical Is the task on the critical path.
     * @return Ascii representation of the task. Length is the duration * charPerTimeUnit.
     */
    String formatTask(Task t, int charPerTimeUnit, boolean isCritical) {
        StringBuilder sb = new StringBuilder();
        String fill = isCritical ? "*" : "-";
        int dur = instance.duration(t);
        int machine = instance.machine(t);
        int stringLength = dur * charPerTimeUnit;
        int charsForMachine = machine < 10 ? 1 : 2;
        int numSpaces = stringLength - 2 - charsForMachine; // we use 2 chars for '[' and '[' + 1 or 2 for the machine number
        int startSpaces = numSpaces / 2;
        int endSpaces = numSpaces - startSpaces;
        sb.append("[");
        sb.append(fill.repeat(startSpaces - 1));
        sb.append(" ");
        sb.append(machine);
        sb.append(" ");
        sb.append(fill.repeat(endSpaces - 1));
        sb.append("]");
        return sb.toString();
    }


    @Override
    public Optional<Schedule> toSchedule() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        if (times.length != schedule.times.length) {
            return false;
        }
        for(int i=0; i<times.length; i++) {
            if (!Arrays.equals(times[i], schedule.times[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.stream(times).map(Arrays::hashCode).collect(Collectors.toList()).hashCode();

    }
}
