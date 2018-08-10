package com.inspectionDiff;

public class XmlDiffResult {
    public int baseCount;
    public int updatedCount;
    public int baseFiltered;
    public int updatedFiltered;
    public int baseProblems;
    public int updatedProblems;
    public int added;
    public int removed;

    public void add(XmlDiffResult other) {
        baseCount += other.baseCount;
        updatedCount += other.updatedCount;
        baseFiltered += other.baseFiltered;
        updatedFiltered += other.updatedFiltered;
        baseProblems += other.baseProblems;
        updatedProblems += other.updatedProblems;
        added += other.added;
        removed += other.removed;
    }

}

