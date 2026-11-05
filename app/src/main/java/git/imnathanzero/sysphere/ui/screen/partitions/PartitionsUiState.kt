package git.imnathanzero.sysphere.ui.screen.partitions

data class PartitionsUiState(
    val isRoot: Boolean = false,
    val isLoading: Boolean = false,
    val partitions: List<PartitionInfo> = emptyList(),
    val errorMessage: String = "",
    val successMessage: String = "",
    val partitionToDelete: PartitionInfo? = null,
    val partitionToOverride: PartitionInfo? = null,
    val hasLoopSupport: Boolean = true,
)
