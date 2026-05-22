# AWS Serverless Image Analyzer

S3 → Lambda (Java 17) → Rekognition → DynamoDB pipeline that detects objects and identifies celebrities.

## Features
- **Object Detection**: Uses `DetectLabels` to identify objects/scenes
- **Celebrity Recognition**: Uses `RecognizeCelebrities` API
- **Serverless**: Zero infrastructure management with AWS Lambda
- **Error Handling**: Handles URL-encoded S3 keys and invalid image formats
- **Persistence**: Stores results with timestamp in DynamoDB

## Tech Stack
Java 17, Maven, AWS SDK v2, Lambda, S3, Rekognition, DynamoDB, IAM

## Setup
1. Create S3 bucket + DynamoDB table `ImageAnalysis` with `imageName` as partition key
2. Deploy Lambda JAR with IAM role permissions for Rekognition/S3/DynamoDB
3. Add S3 trigger for `ObjectCreated` events

## Demo
![DynamoDB Result](Dynamodb%20Result.png)

## Live API Endpoint
`POST https://u55ndx8d52.execute-api.us-east-1.amazonaws.com/analyze`

## Sample Output
| imageName | labels | celebrities |
| --- | --- | --- |
| elon.jpeg | ["Person", "Suit", "Official"] | ["Elon Musk"] |
