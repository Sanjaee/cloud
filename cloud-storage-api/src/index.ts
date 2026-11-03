import { app } from './app';
import { env } from './config/env';

// When running locally under Bun, start server
if (!process.env.VERCEL) {
  const port = Number(env.PORT) || 3000;
  app.listen(port, () => {
    console.log(`🚀 Zacode Cloud Storage API running at http://localhost:${port}`);
    console.log(`📚 OpenAPI / Swagger documentation: http://localhost:${port}/swagger`);
  });
}

// Vercel serverless function export
export default app;
