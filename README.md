# Quiz Validator - Real-time Leaderboard System

## 🎯 Overview
This application polls a quiz validator API, handles duplicate responses, aggregates scores, and submits a correct leaderboard.

## ✨ Features
- **10 API polls** with 5-second delay
- **Smart deduplication** using (roundId + participant)
- **Real-time dashboard** with live updates
- **Automatic score aggregation**
- **One-click submission**

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- Maven (or use the provided JAR)

### Running the Application

1. **Clone the repository**
```bash
git clone https://github.com/your-username/quiz-validator.git
cd quiz-validator
