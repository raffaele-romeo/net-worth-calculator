
type StatusBadgeProps = {
    label: string;
    ok: boolean;
    loading?: boolean
};

export default function StatusBadge ({ label, ok, loading }: StatusBadgeProps) {
    if (loading) return <span>{label}: ...</span>;
    return <span>{label}: {ok ? "Connected" : "Down"}</span>;
}