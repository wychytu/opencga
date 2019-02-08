package org.opencb.opencga.storage.core.metadata.models;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 10/01/19.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class SampleMetadata extends StudyResourceMetadata<SampleMetadata> {

    private Set<Integer> files;
//    private Set<Integer> cohorts;

    public SampleMetadata() {
        files = new HashSet<>();
//        cohorts = new HashSet<>();
    }

    public SampleMetadata(int studyId, int id, String name) {
        super(studyId, id, name);
        files = new HashSet<>();
//        cohorts = new HashSet<>();
    }

    public Set<Integer> getFiles() {
        return files;
    }

    public SampleMetadata setFiles(Set<Integer> files) {
        this.files = files;
        return this;
    }

//    public Set<Integer> getCohorts() {
//        return cohorts;
//    }
//
//    public SampleMetadata setCohorts(Set<Integer> cohorts) {
//        this.cohorts = cohorts;
//        return this;
//    }


    public TaskMetadata.Status getIndexStatus() {
        return getStatus("index");
    }

    public SampleMetadata setIndexStatus(TaskMetadata.Status indexStatus) {
        return setStatus("index", indexStatus);
    }

    public boolean isIndexed() {
        return isReady("index");
    }

    public boolean isAnnotated() {
        return TaskMetadata.Status.READY.equals(getAnnotationStatus());
    }

    public TaskMetadata.Status getAnnotationStatus() {
        return getStatus("annotation");
    }

    public SampleMetadata setAnnotationStatus(TaskMetadata.Status annotationStatus) {
        return setStatus("annotation", annotationStatus);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("studyId", getStudyId())
                .append("id", getId())
                .append("name", getName())
                .append("status", getStatus())
                .append("files", files)
//                .append("cohorts", cohorts)
                .toString();
    }
}
