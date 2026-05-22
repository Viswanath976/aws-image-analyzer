# AWS Serverless Image Analyzer

S3 → Lambda (Java 17) → Rekognition → DynamoDB pipeline that detects objects and identifies celebrities.

## Features

- AWS Rekognition `DetectLabels` + `RecognizeCelebrities`
- Stores results with timestamp in DynamoDB
- Handles S3 key URL decoding and invalid images
- Built with AWS SDK v2

## Tech Stack

Java 17, Maven, AWS Lambda, S3, Rekognition, DynamoDB, IAM

## Setup

1. Create S3 bucket + DynamoDB table `ImageAnalysis` with `imageName` as partition key
2. Deploy Lambda JAR with IAM role for Rekognition/S3/DynamoDB
3. Add S3 trigger for `ObjectCreated` events
## Demo 
Dynamodb Result.png
