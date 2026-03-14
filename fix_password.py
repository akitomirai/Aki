#!/usr/bin/env python3
import subprocess
import sys

# 正确的BCrypt哈希
hashed_password = "$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW"
print(f"Password hash: {hashed_password}")
print(f"Length: {len(hashed_password)}")

# 演示账号列表
demo_accounts = ["platform", "platform_admin", "entA_admin", "entA_user", "entB_admin", "reg1", "reg2", "regulator"]

# 构建SQL更新语句
sql = f"UPDATE sys_user SET password = '{hashed_password}' WHERE username IN ({','.join(['%s'] * len(demo_accounts))});"
print(f"SQL: {sql % tuple(demo_accounts)}")

# 执行MySQL命令
cmd = [
    "mysql",
    "-h", "localhost",
    "-u", "root",
    "-p1154228715",
    "traceability_cs",
    "-e",
    f"UPDATE sys_user SET password = '{hashed_password}' WHERE username IN ('platform','platform_admin','entA_admin','entA_user','entB_admin','reg1','reg2','regulator')"
]

print("Executing MySQL update...")
result = subprocess.run(cmd, capture_output=True, text=True)
print(f"Return code: {result.returncode}")
if result.stderr:
    print(f"Error: {result.stderr}")
else:
    print("Update completed successfully")
    
# 验证更新
verify_cmd = [
    "mysql",
    "-h", "localhost",
    "-u", "root", 
    "-p1154228715",
    "traceability_cs",
    "-e",
    "SELECT username, LENGTH(password) as len, LEFT(password, 30) as prefix FROM sys_user WHERE username IN ('platform','entA_admin','reg1')"
]

print("\nVerifying update...")
result = subprocess.run(verify_cmd, capture_output=True, text=True)
print(result.stdout)