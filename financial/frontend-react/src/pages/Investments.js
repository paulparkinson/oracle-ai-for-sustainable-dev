import React, { useState } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Allow scrolling if content overflows */
`;

const SidePanel = styled.div`
  border: 1px solid #444; /* Darker border */
  padding: 10px;
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the side panel */
  color: #ffffff; /* Light text */
  margin-bottom: 20px; /* Add spacing below the side panel */
`;

const ToggleButton = styled.button`
  background-color: #1abc9c;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 10px;

  &:hover {
    background-color: #16a085;
  }
`;

const CollapsibleContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`;

const TextContent = styled.div`
  flex: 1;
  margin-right: 20px; /* Add spacing between text and video */
`;

const VideoWrapper = styled.div`
  flex-shrink: 0;
  width: 40%; /* Set the width of the video */
`;

const IframeContainer = styled.div`
  width: 100%;
  height: 100%;
  overflow: hidden;
`;

const Iframe = styled.iframe`
  width: 100%;
  height: calc(100vh - 20px); // Adjust height to fit the layout
  border: none;
`;

const Investments = () => {
  const [isCollapsed, setIsCollapsed] = useState(true);

  return (
    <PageContainer>
      <h2>Process: Get personal financial insights</h2>
      <h2>Tech: Vector Search, AI Agents and MCP</h2>
      <h2>Reference: DMCC</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!isCollapsed && (
          <CollapsibleContent>
            <TextContent>
              <div>
                <a
                  href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier-financial/index.html"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Click here for workshop lab and further information
                </a>
              </div>
              <div>
                <a
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Direct link to source code on GitHub
                </a>
              </div>
              <h4>Financial Process:</h4>
              <ul>
                <li>Analyze investment portfolios</li>
                <li>Track performance of stocks, bonds, and mutual funds</li>
                <li>Generate insights using Oracle Analytics</li>
              </ul>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Leverage Oracle Analytics Cloud for data visualization</li>
                <li>Use Oracle Database for portfolio data storage</li>
                <li>Integrate with external APIs for real-time market data</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Vector processing in the same database and with other business data (structured and unstructured)</li>
              </ul>
            </TextContent>
            <VideoWrapper>
            <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="315"
                src="https://www.youtube.com/embed/E1pOaCkd_PM"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: '1px solid #444' }}
              ></iframe>
            </VideoWrapper>
          </CollapsibleContent>
        )}
      </SidePanel>

      <h2>Investment Portfolio Management</h2>
      <p>
        Manage and analyze your investment portfolio with Oracle's advanced analytics and database solutions.<br />
        Track real-time performance and generate actionable insights.
      </p>

      <IframeContainer>
        <Iframe src="http://141.148.204.74:8080" title="Investments" />
      </IframeContainer>
    </PageContainer>
  );
};

export default Investments;
