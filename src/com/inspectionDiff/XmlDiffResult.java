package com.inspectionDiff;

class XmlDiffResult {
    int baseCount;
    int updatedCount;
    int baseFiltered;
    int updatedFiltered;
    int baseProblems;
    int updatedProblems;
    int added;
    int removed;

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

