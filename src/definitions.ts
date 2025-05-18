export interface CapacitorMapSdkPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
