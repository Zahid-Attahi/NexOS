public enum ProcessState {
    OutboxFull,
    InboxEmpty,
    QuantumExpired,
    Runnable
}