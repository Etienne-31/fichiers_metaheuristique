package jobshop.encodings;

import java.util.Objects;

/** Represents a task (job,task) of a jobshop problem.
 *
 * Example : (2, 3) represents the fourth task of the third job. (remember that we start counting at 0)
 **/
public final class Task {

    /** Identifier of the job */
    public final int job;

    /** Index of the task inside the job. */
    public final int task;

    /** Creates a new Task object (job, task). */
    public Task(int job, int task) {
        this.job = job;
        this.task = task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task1 = (Task) o;
        return job == task1.job &&
                task == task1.task;
    }

    @Override
    public int hashCode() {
        return Objects.hash(job, task);
    }

    @Override
    public String toString() {
        return "(" + job +", " + task + ')';
    }
}
