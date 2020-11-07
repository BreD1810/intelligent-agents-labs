package group8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

/**
 * A simple example agent that makes random bids above a minimum target utility. 
 *
 * @author Tim Baarslag
 */
public class JohnyBlack extends AbstractNegotiationParty
{
	private static double MINIMUM_TARGET = 0.8;
	private Bid lastOffer;
	private HashMap<Integer, HashMap<String, Integer>> frequencyTable = new HashMap<>();
	private int noBids = 0;

	/**
	 * Initializes a new instance of the agent.
	 */
	@Override
	public void init(NegotiationInfo info) 
	{
		super.init(info);
		AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
		AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

		List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();

		for (Issue issue : issues) {
			int issueNumber = issue.getNumber();
			System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

			// Assuming that issues are discrete only
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

			HashMap<String, Integer> issueHashMap = new HashMap<>();

			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				String value = valueDiscrete.getValue();
				issueHashMap.put(value, 0);
				System.out.println(value);
				System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
				try
				{
					System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			frequencyTable.put(issueNumber, issueHashMap);
		}
	}

	/**
	 * Makes a random offer above the minimum utility target
	 * Accepts everything above the reservation value at the very end of the negotiation; or breaks off otherwise. 
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions) 
	{
		// Check for acceptance if we have received an offer
		if (lastOffer != null)
			if (timeline.getTime() >= 0.99)
				if (getUtility(lastOffer) >= utilitySpace.getReservationValue()) 
					return new Accept(getPartyId(), lastOffer);
				else
					return new EndNegotiation(getPartyId());
		
		// Otherwise, send out a random offer above the target utility 
		return new Offer(getPartyId(), generateRandomBidAboveTarget());
	}

	private Bid generateRandomBidAboveTarget() 
	{
		Bid randomBid;
		double util;
		int i = 0;
		// try 100 times to find a bid under the target utility
		do 
		{
			randomBid = generateRandomBid();
			util = utilitySpace.getUtility(randomBid);
		} 
		while (util < MINIMUM_TARGET && i++ < 100);		
		return randomBid;
	}

	/**
	 * Remembers the offers received by the opponent.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action) 
	{
		if (action instanceof Offer) 
		{
			lastOffer = ((Offer) action).getBid();

			System.out.println("Received offer: " + lastOffer.toString());
			noBids += 1;
			// Update frequency table
			List<Issue> issues = lastOffer.getIssues();

			for (Issue issue : issues) {
				int issueNumber = issue.getNumber();
				String value = ((ValueDiscrete) lastOffer.getValue(issueNumber)).getValue();
				int currentCount = frequencyTable.get(issueNumber).get(value);
				frequencyTable.get(issueNumber).put(value, currentCount + 1);
			}

			printFrequencyTable();
			double predictedValue = predictValuation(lastOffer, issues);
			System.out.println("Predicted value: " + predictedValue);
		}
	}

	/**
	 * Predict the valuation of an offer for an opponent.
	 * @param offer The offer the opponent has made
	 * @param issues The issues that the offer negotiates over
	 * @return
	 */
	private double predictValuation(Bid offer, List<Issue> issues)
	{
		double value = 0;
		double[] optionValues = getOptionValues(offer, issues);
		double[] normalisedWeights = getNormalisedWeights(issues);
		for (int i = 0; i < optionValues.length; i++)
			value += optionValues[i] * normalisedWeights[i];

		return value;
	}

	/**
	 * Calculate the value of an opponent's options
	 * @param bid The bid provided by the opponent
	 * @param issues A list of issues that the bid negotiates about
	 * @return A list of option values, calculated using preference order
	 */
	private double[] getOptionValues(Bid bid, List<Issue> issues) {
		double[] optionValues = new double[issues.size()];
		for (int i = 0; i < issues.size(); i++)
		{
			Issue issue = issues.get(i);
			int issueNumber = issue.getNumber();
			HashMap<String, Integer> options = frequencyTable.get(issueNumber);
			double noOptions = options.keySet().size();

			String chosenOption = ((ValueDiscrete) bid.getValue(issueNumber)).getValue();
			int rank = 0;
			int optionValue = options.get(chosenOption);
			for (String option : options.keySet())
			{
				if (options.get(option) >= optionValue)
					rank += 1;
			}

			optionValues[i] = (noOptions - rank + 1) / noOptions;
		}

		return optionValues;
	}

	/**
	 * Get the normalised weights for each issue, using the Gini Index
	 * @param issues A list of issues that the bid negotiates about
	 * @return An array of weights
	 */
	private double[] getNormalisedWeights(List<Issue> issues) {
		double[] weights = new double[issues.size()];
		double noBidsSquared = Math.pow(noBids, 2);

		for (int i = 0; i < issues.size(); i++)
		{
			List<Integer> optionCounts = new ArrayList<>(frequencyTable.get(issues.get(i).getNumber()).values());
			double weight = 0;
			for (Integer option : optionCounts)
				weight += (Math.pow(option, 2) / noBidsSquared);

			weights[i] = weight;
		}

		double weightSum = Arrays.stream(weights).sum();
		double[] normalisedWeights = new double[issues.size()];
		for (int i = 0; i < weights.length; i++)
			normalisedWeights[i] = weights[i] / weightSum;

		return normalisedWeights;
	}

	private void printFrequencyTable() {
		System.out.println("Frequency table");
		for (Integer issueNo : frequencyTable.keySet()) {
			System.out.print("Issue" + issueNo + " ");
			for (String option : frequencyTable.get(issueNo).keySet()) {
				System.out.print(frequencyTable.get(issueNo).get(option) + " ");
			}
			System.out.println();
		}
	}

	@Override
	public String getDescription() 
	{
		return "Based on the Johny Black Agent";
	}

	/**
	 * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
	 */
	@Override
	public AbstractUtilitySpace estimateUtilitySpace() 
	{
		return super.estimateUtilitySpace();
	}

}
