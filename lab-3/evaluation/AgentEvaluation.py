# ---
# jupyter:
#   jupytext:
#     formats: ipynb,py:percent
#     text_representation:
#       extension: .py
#       format_name: percent
#       format_version: '1.3'
#       jupytext_version: 1.5.2
#   kernelspec:
#     display_name: 'Python 3.8.6 64-bit (''evaluation'': pipenv)'
#     metadata:
#       interpreter:
#         hash: 3cdd0439fe8cdee320e0542e685b3b057a20c02ef4e0c6b77aa6dd74b17b07b4
#     name: 'Python 3.8.6 64-bit (''evaluation'': pipenv)'
# ---

# %%
import matplotlib.pyplot as plt
import csv
import pandas as pd
import numpy as np

# negotiations = []

# with open('../log/log1.csv', mode='r') as csv_file:
#     csv_reader = csv.DictReader(csv_file, delimiter=';')
#     line_count = 0
#     for row in csv_reader:
#         if line_count == 0 or line_count == 1:
#             line_count += 1
#             print(row)
#         else:
#             if "JohnyBlack" in row["Agent 1"]:
#                 negotiation = {"paretoDist": row["Dist. to Pareto"], "nashDist": row["Dist. to Nash"], "socialWelfare": row["Social Welfare"]}
#                 utils = {"jonnyUtil": row["Utility 1"], "otherUtil": row["Utility 2"]}
#                 negotiation.update(utils)
#                 negotiations.append(negotiation)
#             elif "JohnyBlack" in row["Agent 2"]:
#                 negotiation = {"paretoDist": row["Dist. to Pareto"], "nashDist": row["Dist. to Nash"], "socialWelfare": row["Social Welfare"]}
#                 utils = {"jonnyUtil": row["Utility 2"], "otherUtil": row["Utility 1"]}
#                 negotiation.update(utils)
#                 negotiations.append(negotiation)
#             else:
#                 continue


# %%
cols = ["Dist. to Pareto", "Dist. to Nash", "Social Welfare", "Agent 1", "Agent 2", "Utility 1", "Utility 2"]
df = pd.read_csv('../log/log1.csv', sep=';', usecols=cols)
df

# %%
df = df.loc[df['Agent 1'].str.contains("JohnyBlack") | df['Agent 2'].str.contains("JohnyBlack")]
df

# %%
data = df.to_numpy()

paretoPointsX = []
paretoPointsY = []
otherX = []
otherY = []

for row in data:
    if row[0] == 0:
        if "JohnyBlack" in row[3]:
            paretoPointsX.append(row[5])
            paretoPointsY.append(row[6])
        else:
            paretoPointsX.append(row[6])
            paretoPointsY.append(row[5])
    else:
        if "JohnyBlack" in row[3]:
            otherX.append(row[5])
            otherY.append(row[6])
        else:
            otherX.append(row[6])
            otherY.append(row[5])

fig,ax = plt.subplots()
ax.scatter(paretoPointsX, paretoPointsY, c="blue", label="Pareto")
ax.scatter(otherX, otherY, c="red", label="Non-Pareto")
ax.legend()
# plt.plot(paretoPointsX, paretoPointsY, "bs")
# plt.plot(otherX,otherY, "yo")
# plt.
plt.xlabel("Jonny Black Utility")
plt.ylabel("Other Utility")
plt.title("Johny Black vs Others Tournament")


# %%
