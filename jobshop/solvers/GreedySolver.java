package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.HashMap;


import java.util.Optional;

/** An empty shell to implement a greedy solver. */
public class GreedySolver implements Solver {

    /** All possible priorities for the greedy solver. */
    public enum Priority {
        SPT, LPT, SRPT, LRPT, EST_SPT, EST_LPT, EST_SRPT, EST_LRPT
    }

    /** Priority that the solver should use. */
    final Priority priority;

    /** Creates a new greedy solver that will use the given priority. */
    public GreedySolver(Priority p) {
        this.priority = p;
    }

    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {
        int nb_tache_per_job = instance.numTasks;
        int nb_job_instance = instance.numJobs;
        ResourceOrder newRorder = new ResourceOrder(instance);;

        ArrayList<Task> task_to_exec = new ArrayList<Task>();

        if(nb_tache_per_job > 0){
            for(int job = 0 ;job < nb_job_instance;job++){
                task_to_exec.add(new Task(job,0));
            }
        }



        //VAleur d'initialisation pour EST

        ArrayList<Integer> dispo_machine = new ArrayList<Integer>();
        ArrayList<Integer> dispo_job = new ArrayList<Integer>();

        boolean same_value = true;


        for(int i =0;i<instance.numMachines;i++){
            dispo_machine.add(i,0);

        }

        for(int i =0;i<instance.numJobs;i++){
            dispo_job.add(i,0);
        }

        while(!task_to_exec.isEmpty()){
            //task_to_exec.forEach(System.out::println);
           // System.out.println("empty" + task_to_exec.isEmpty());
            //System.out.println("size" + task_to_exec.size());
            //System.out.println("-------------");
            switch (this.priority){

                case SPT:
                    Task shortest_task = task_to_exec.get(0);
                    for(Task task_iterator : task_to_exec){
                      //  System.out.println("A cette itération la liste est de taille "+task_to_exec.size());
                        if(instance.duration(task_iterator) <= instance.duration(shortest_task)){
                            shortest_task = task_iterator;
                        }
                    }
                    task_to_exec.remove(shortest_task);
                   // System.out.println("On supprime la shortest task de la liste,  la liste est de taille "+task_to_exec.size()+" On retire la tâche "+shortest_task.toString());
                    if((shortest_task.task + 1) < nb_tache_per_job){
                        Task task_ajouter = new Task(shortest_task.job,shortest_task.task+1);
                        task_to_exec.add(task_ajouter);
                      //  System.out.println("On ajoute la new task la liste est de taille "+task_to_exec.size()+" On a ajouté la task "+task_ajouter.toString());
                    }
                    newRorder.addTaskToMachine(instance.machine(shortest_task),shortest_task);

                    shortest_task = null;

                    break;


                case LRPT:
                    int LR_process_duree = 0;
                    Task  LRProcess = task_to_exec.get(0);
                    for(int i = LRProcess.task;i<=nb_tache_per_job-1;i++){
                        LR_process_duree = LR_process_duree + instance.duration(LRProcess.job,i);
                    }

                  //  System.out.println("Après initialisation de l'itération la LRPT est "+LRProcess.toString()+" Dont la durée est "+LR_process_duree);


                    for(Task task_iterator : task_to_exec){

                        int tache_iterator_duree = 0;

                        for(int ite = task_iterator.task;ite<=nb_tache_per_job-1;ite++){
                            tache_iterator_duree = tache_iterator_duree + instance.duration(task_iterator.job,ite);
                        }

                        if(tache_iterator_duree > LR_process_duree){
                            LRProcess = task_iterator;
                            LR_process_duree = tache_iterator_duree;

                        }


                    }
                    //System.out.println("A la fin de la boucle for le  LRPT est "+LRProcess.toString()+" Dont la durée est "+LR_process_duree+" On va donc le retirer ");
                    task_to_exec.remove(LRProcess);

                    if((LRProcess.task + 1) < nb_tache_per_job){
                        Task task_ajouter = new Task(LRProcess.job,LRProcess.task+1);
                        task_to_exec.add(task_ajouter);
                        //  System.out.println("On ajoute la new task la liste est de taille "+task_to_exec.size()+" On a ajouté la task "+task_ajouter.toString());
                    }
                    newRorder.addTaskToMachine(instance.machine(LRProcess),LRProcess);

                    LRProcess = null;

                    break;


                case EST_SPT:

                    Task EST = task_to_exec.get(0);
                    int EST_start_time = Math.max(dispo_job.get(EST.job),dispo_machine.get(instance.machine(EST)));

                    for(Task task_iterator : task_to_exec){
                        int task_iterator_start_time = Math.max(dispo_job.get(task_iterator.job),dispo_machine.get(instance.machine(task_iterator)));

                        if(task_iterator_start_time < EST_start_time){
                            EST_start_time = task_iterator_start_time;
                            EST = task_iterator;
                        }
                        else if(task_iterator_start_time == EST_start_time){
                            if(instance.duration(task_iterator) < instance.duration(EST)){
                                EST_start_time = task_iterator_start_time;
                                EST = task_iterator;
                            }
                        }
                    }
                    task_to_exec.remove(EST);

                    if((EST.task + 1) < nb_tache_per_job){
                        Task task_ajouter = new Task(EST.job,EST.task+1);
                        task_to_exec.add(task_ajouter);
                        //  System.out.println("On ajoute la new task la liste est de taille "+task_to_exec.size()+" On a ajouté la task "+task_ajouter.toString());
                    }
                    newRorder.addTaskToMachine(instance.machine(EST),EST);
                    //System.out.println(EST);

                    //Maj pour MAchine et Job = EST_start_time + duration(EST))

                    dispo_machine.set(instance.machine(EST),EST_start_time+instance.duration(EST));
                    dispo_job.set(EST.job,EST_start_time+instance.duration(EST));



                    break;

                case EST_LPT:
                    EST = task_to_exec.get(0);
                    EST_start_time = Math.max(dispo_job.get(EST.job),dispo_machine.get(instance.machine(EST)));

                    for(Task task_iterator : task_to_exec){
                        int task_iterator_start_time = Math.max(dispo_job.get(task_iterator.job),dispo_machine.get(instance.machine(task_iterator)));

                        if(task_iterator_start_time < EST_start_time){
                            EST_start_time = task_iterator_start_time;
                            EST = task_iterator;
                        }
                        else if(task_iterator_start_time == EST_start_time){
                            if(instance.duration(task_iterator) > instance.duration(EST)){
                                EST_start_time = task_iterator_start_time;
                                EST = task_iterator;
                            }
                        }
                    }
                    task_to_exec.remove(EST);

                    if((EST.task + 1) < nb_tache_per_job){
                        Task task_ajouter = new Task(EST.job,EST.task+1);
                        task_to_exec.add(task_ajouter);
                        //  System.out.println("On ajoute la new task la liste est de taille "+task_to_exec.size()+" On a ajouté la task "+task_ajouter.toString());
                    }
                    newRorder.addTaskToMachine(instance.machine(EST),EST);
                    //System.out.println(EST);

                    //Maj pour MAchine et Job = EST_start_time + duration(EST))

                    dispo_machine.set(instance.machine(EST),EST_start_time+instance.duration(EST));
                    dispo_job.set(EST.job,EST_start_time+instance.duration(EST));

            }




        }


        return newRorder.toSchedule();
    }
}
