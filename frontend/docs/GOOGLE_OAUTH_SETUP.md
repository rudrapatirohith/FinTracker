# Google OAuth Setup Instructions

To enable Google OAuth login in your FinanceTracker application, follow these steps:

## 1. Configure Google Cloud Console

### Step 1: Create a Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google+ API for your project

### Step 2: Configure OAuth Consent Screen
1. Navigate to "APIs & Services" > "OAuth consent screen"
2. Choose "External" user type
3. Fill in the required information:
   - App name: "FinanceTracker"
   - User support email: Your email
   - Developer contact information: Your email
4. Add your domain to "Authorized domains" if using a custom domain
5. Add the following scopes:
   - `../auth/userinfo.email`
   - `../auth/userinfo.profile`
   - `openid`

### Step 3: Create OAuth 2.0 Credentials
1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Choose "Web application"
4. Add authorized JavaScript origins:
   - `http://localhost:3000` (for development)
   - Your production domain (e.g., `https://yourapp.com`)
5. Add authorized redirect URIs:
   - `http://localhost:3000/auth/callback` (for development)
   - `https://yourapp.com/auth/callback` (for production)
   - Your Supabase project callback URL: `https://your-project-ref.supabase.co/auth/v1/callback`

## 2. Configure Supabase

### Step 1: Enable Google Provider
1. Go to your [Supabase Dashboard](https://supabase.com/dashboard)
2. Navigate to "Authentication" > "Providers"
3. Find "Google" and toggle it on
4. Enter your Google OAuth credentials:
   - **Client ID**: From Google Cloud Console
   - **Client Secret**: From Google Cloud Console

### Step 2: Configure Redirect URLs
1. In Supabase Dashboard, go to "Authentication" > "URL Configuration"
2. Add your site URL and redirect URLs:
   - Site URL: `http://localhost:3000` (development) or your production URL
   - Redirect URLs: Add your callback URLs

## 3. Environment Variables

Make sure your `.env.local` file contains:

\`\`\`env
NEXT_PUBLIC_SUPABASE_URL=your_supabase_project_url
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_supabase_anon_key
\`\`\`

## 4. Testing

1. Start your development server: `npm run dev`
2. Navigate to the login page
3. Click "Continue with Google"
4. Complete the OAuth flow
5. You should be redirected to the dashboard upon successful authentication

## Troubleshooting

### Common Issues:

1. **"provider not enabled" error**: 
   - Ensure Google provider is enabled in Supabase Dashboard
   - Check that Client ID and Secret are correctly entered

2. **Redirect URI mismatch**:
   - Verify redirect URIs in Google Cloud Console match your callback URLs
   - Ensure Supabase callback URL is added to Google Cloud Console

3. **CORS errors**:
   - Add your domain to authorized JavaScript origins in Google Cloud Console
   - Check Supabase URL configuration

4. **Development vs Production**:
   - Use different OAuth clients for development and production
   - Update redirect URIs accordingly

## Security Notes

- Never commit your Google Client Secret to version control
- Use environment variables for sensitive configuration
- Regularly rotate your OAuth credentials
- Monitor OAuth usage in Google Cloud Console

For more detailed information, refer to:
- [Supabase Auth with Google Documentation](https://supabase.com/docs/guides/auth/social-login/auth-google)
- [Google OAuth 2.0 Documentation](https://developers.google.com/identity/protocols/oauth2)
