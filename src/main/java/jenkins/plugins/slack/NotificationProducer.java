package jenkins.plugins.slack;

public interface NotificationProducer<I, O> {

    O startBuild(I build);
    O finalizeBuild(I build);
    O completedBuild(I build);

}
