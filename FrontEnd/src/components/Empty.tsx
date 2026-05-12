interface EmptyProps {
  label: string;
}

export default function Empty({ label }: EmptyProps) {
  return (
    <div className="font-mono flex items-center justify-center w-full py-3 px-4 rounded-lg border border-dashed border-(--color-border-strong) text-[12px] text-(--color-text-muted) tracking-wider">
      ◌ {label}
    </div>
  );
}
