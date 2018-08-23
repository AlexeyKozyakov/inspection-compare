package com.intellij.plugins.inspectioncompare.diff;

public class XmlDiffResult {
    public int count;
    public int filteredCount;
    public int baseProblems;
    public int updatedProblems;
    public int added;
    public int removed;

    void add(XmlDiffResult other) {
        count += other.count;
        filteredCount += other.filteredCount;
        baseProblems += other.baseProblems;
        updatedProblems += other.updatedProblems;
        added += other.added;
        removed += other.removed;
    }

}

