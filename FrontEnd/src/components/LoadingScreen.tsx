function LoadingScreen() {
  return (
    <div className="relative flex flex-col items-center justify-center min-h-screen w-screen overflow-hidden bg-(--color-bg-main) text-(--color-text-main)">
      <div className="absolute inset-0 pointer-events-none opacity-60 grid-texture" />
      <div className="absolute pointer-events-none rounded-full cyan-orb" />

      <div className="relative z-10 flex flex-col items-center gap-4">
        <div className="w-10 h-10 rounded-xl bg-(--color-primary-light) border border-(--color-border-accent) flex items-center justify-center animate-pulse">
          <span className="text-lg text-(--color-primary)">⬡</span>
        </div>
        <p className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest animate-pulse">
          loading...
        </p>
      </div>
    </div>
  );
}

export default LoadingScreen;
