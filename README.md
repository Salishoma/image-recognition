# Image Rekognition Service

## Overview

This project provides a Spring Boot backend for integrating with **Amazon Rekognition Face Liveness**. It exposes APIs for:

* Creating a Face Liveness session
* Retrieving Face Liveness results
* Comparing faces (optional)
* Managing the complete Face Liveness workflow

The project is intended to be reusable across multiple applications.

---

# Prerequisites

Before running this project, ensure you have:

* Java 21+
* Maven 3.9+
* An AWS Account
* AWS IAM permissions
* Amazon Rekognition enabled
* Amazon Cognito Identity Pool (for web/mobile liveness)
* AWS credentials configured locally or in the deployment environment

---

# AWS Setup

## Step 1 - Create an IAM User

Navigate to:

```
AWS Console
→ IAM
→ Users
→ Create User
```

Example:

```
image-rekognition-user
```

Grant the user programmatic access.

---

## Step 2 - Create an IAM Policy

Create a custom policy containing the following permissions.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "RekognitionLiveness",
      "Effect": "Allow",
      "Action": [
        "rekognition:CreateFaceLivenessSession",
        "rekognition:GetFaceLivenessSessionResults",
        "rekognition:CompareFaces"
      ],
      "Resource": "*"
    }
  ]
}
```

Attach the policy to the IAM user.

---

## Step 3 - Generate Access Keys

Navigate to:

```
IAM
→ Users
→ Security Credentials
→ Create Access Key
```

Store:

```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

These credentials are required by the backend.

---

# Cognito Identity Pool Setup

Amazon Rekognition Face Liveness requires temporary AWS credentials for browser clients.

## Create Identity Pool

Navigate to

```
AWS Console
→ Amazon Cognito
→ Identity Pools
→ Create Identity Pool
```

Create an Identity Pool in the **same AWS Region** as Rekognition.

Example:

```
us-east-1
```

---

## Enable Guest Access

For onboarding scenarios where users are **not authenticated**, enable Guest (Unauthenticated) access.

---

## Create Guest Role

Create an IAM Role trusted by Cognito Identity.

Trust Policy

Replace the Identity Pool ID below with your own.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "cognito-identity.amazonaws.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "cognito-identity.amazonaws.com:aud": "<IDENTITY_POOL_ID>"
        },
        "ForAnyValue:StringLike": {
          "cognito-identity.amazonaws.com:amr": "unauthenticated"
        }
      }
    }
  ]
}
```

---

## Guest Role Permissions

Attach the following policy to the Guest Role.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "rekognition:CreateFaceLivenessSession",
        "rekognition:GetFaceLivenessSessionResults"
      ],
      "Resource": "*"
    }
  ]
}
```

Attach this IAM role as the **Guest Role** inside the Identity Pool.

---

# Region Requirements

The following resources **must all be created in the same AWS Region**.

* Amazon Rekognition
* Cognito Identity Pool
* S3 Bucket (if used)
* Frontend configuration

Example

```
us-east-1
```

Using different regions will result in errors such as:

```
InvalidIdentityPoolConfigurationException

Region not supported

No credentials provided
```

---

# Backend Configuration

Configure the following environment variables.

```properties
AWS_REGION=us-east-1

AWS_ACCESS_KEY_ID=xxxxxxxx

AWS_SECRET_ACCESS_KEY=xxxxxxxx
```

---

# Frontend Configuration

The frontend requires the Identity Pool ID to obtain temporary AWS credentials.

Example credential provider:

```typescript
import { fromCognitoIdentityPool } from "@aws-sdk/credential-providers";

const credentialProvider = async () => {
    return fromCognitoIdentityPool({
        identityPoolId: "us-east-1:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
        clientConfig: {
            region: "us-east-1"
        }
    })();
};
```

Example usage:

```tsx
<FaceLivenessDetectorCore
    sessionId={sessionId}
    region="us-east-1"
    config={{
        credentialProvider
    }}
/>
```

---

# Backend Flow

1. Client requests a Face Liveness session.
2. Backend calls:

```
CreateFaceLivenessSession
```

3. Backend returns:

* Session ID

4. Frontend launches Face Liveness.

5. Frontend completes liveness.

6. Backend retrieves the result using:

```
GetFaceLivenessSessionResults
```

---

# Mobile (Flutter) Integration

Amazon Rekognition Face Liveness currently has no official Flutter SDK.

Recommended architecture:

```
Flutter App
      │
      ▼
Spring Boot Backend
      │
      ▼
Generate Liveness Session
      │
      ▼
Open WebView / Browser
      │
      ▼
React Face Liveness Page
      │
      ▼
Amazon Rekognition
      │
      ▼
Backend Retrieves Result
      │
      ▼
Redirect User Back To Flutter
```

Use Android App Links or iOS Universal Links to return the user to the mobile application after the liveness process completes.

---

# Common Errors

## InvalidIdentityPoolConfigurationException

Cause

* Guest Role not assigned
* Incorrect trust policy
* Identity Pool configured in a different region

---

## No credentials provided

Cause

Frontend failed to obtain temporary AWS credentials from the Cognito Identity Pool.

---

## Region not supported

Cause

Identity Pool, Rekognition, or S3 bucket are created in different AWS regions.

---

## AccessDeniedException

Cause

IAM user or IAM role lacks required Rekognition permissions.

---

# Security Recommendations

* Never expose AWS Access Keys to frontend clients.
* Only the backend should call `CreateFaceLivenessSession`.
* Use Cognito Identity Pools to issue temporary credentials to browsers.
* Store secrets in AWS Secrets Manager or Azure Key Vault instead of source code.
* Use HTTPS for all frontend and backend communication.

---

# Useful AWS Services

* Amazon Rekognition
* Amazon Cognito Identity Pools
* AWS IAM
* AWS STS
* Amazon S3 (optional)
* CloudWatch Logs

---

# References

* [Amazon Rekognition Face Liveness Documentation](https://docs.aws.amazon.com/rekognition/latest/dg/face-liveness.html)
* [Amazon Cognito Identity Pools Documentation]
* AWS SDK for Java v2
* AWS Amplify Face Liveness Documentation
