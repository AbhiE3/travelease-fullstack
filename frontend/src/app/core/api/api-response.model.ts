export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  error: { code: string; message: string; details?: string[] } | null;
}
