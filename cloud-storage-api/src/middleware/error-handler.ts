import { Elysia } from 'elysia';

export class AppError extends Error {
  constructor(
    public statusCode: number,
    public code: string,
    message: string,
    public details?: any
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export const errorHandler = new Elysia({ name: 'middleware:error-handler' })
  .onError({ as: 'global' }, ({ code, error, set }) => {
    console.error(`[API Error] [${code}]:`, error);

    if ((error as any).name === 'AppError' || (error as any).statusCode) {
      set.status = (error as any).statusCode;
      return {
        success: false,
        error: {
          code: (error as any).code || 'ERROR',
          message: error.message,
          details: (error as any).details,
        },
      };
    }

    if (code === 'VALIDATION') {
      set.status = 400;
      return {
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'The request payload is invalid',
          details: error.all ? error.all : error.message,
        },
      };
    }

    if (code === 'NOT_FOUND') {
      set.status = 404;
      return {
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: 'Requested resource was not found',
        },
      };
    }

    set.status = 500;
    return {
      success: false,
      error: {
        code: 'INTERNAL_SERVER_ERROR',
        message: 'An unexpected internal server error occurred',
      },
    };
  });
